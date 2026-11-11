package co.edu.uco.seguridad.pdp.assignments.application.usecase.impl;

import co.edu.uco.seguridad.pdp.assignments.application.primaryport.request.AssignProfileRequest;
import co.edu.uco.seguridad.pdp.assignments.application.primaryport.request.AssignRoleRequest;
import co.edu.uco.seguridad.pdp.assignments.application.primaryport.response.ProfileAssignmentResponse;
import co.edu.uco.seguridad.pdp.assignments.application.rule.validator.AssignProfileRulesValidator;
import co.edu.uco.seguridad.pdp.assignments.application.secondaryport.repository.ProfileAssignmentRepository;
import co.edu.uco.seguridad.pdp.assignments.application.secondaryport.event.AssignmentOutbox;
import co.edu.uco.seguridad.pdp.assignments.application.event.AssignmentEventFactory;
import co.edu.uco.seguridad.pdp.assignments.domain.Assignment;
import co.edu.uco.seguridad.pdp.assignments.application.usecase.AssignProfileUseCase;
import co.edu.uco.seguridad.pdp.assignments.application.usecase.AssignRoleUseCase;
import co.edu.uco.seguridad.pdp.assignments.domain.ProfileAssignment;
import co.edu.uco.seguridad.pdp.assignments.domain.model.AssignmentId;
import co.edu.uco.seguridad.pdp.assignments.domain.model.ProfileAssignmentId;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import co.edu.uco.seguridad.shared.port.IdentifierGenerator;
import co.edu.uco.seguridad.shared.port.TimeProvider;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.stream.Collectors;
import co.edu.uco.seguridad.pdp.assignments.application.event.AssignmentEventContext;

/**
 * Materializa una {@code Assignment} por cada rol del perfil, reutilizando {@code AssignRoleUseCase}
 * tal cual (mismo módulo, sin cruzar frontera de Modulith) — no repite ninguna de sus reglas. Ver
 * PLAN-HU-011.md hallazgo 1.
 */
public final class AssignProfileUseCaseImpl implements AssignProfileUseCase {

    private final AssignProfileRulesValidator rules;
    private final AssignRoleUseCase assignRoleUseCase;
    private final ProfileAssignmentRepository repository;
    private final IdentifierGenerator identifiers;
    private final TimeProvider time;
    private final AssignmentOutbox outbox; private final AssignmentEventFactory events;

    public AssignProfileUseCaseImpl(AssignProfileRulesValidator rules, AssignRoleUseCase assignRoleUseCase,
                                    ProfileAssignmentRepository repository, IdentifierGenerator identifiers, TimeProvider time) {
        this.rules = Objects.requireNonNull(rules, RequiredArgumentMessages.ASSIGN_PROFILE_RULES_VALIDATOR);
        this.assignRoleUseCase = Objects.requireNonNull(assignRoleUseCase, RequiredArgumentMessages.ASSIGN_ROLE_USE_CASE);
        this.repository = Objects.requireNonNull(repository, RequiredArgumentMessages.PROFILE_ASSIGNMENT_REPOSITORY);
        this.identifiers = Objects.requireNonNull(identifiers, RequiredArgumentMessages.IDENTIFIER_GENERATOR);
        this.time = Objects.requireNonNull(time, RequiredArgumentMessages.TIME_PROVIDER);
        this.outbox = null; this.events = null;
    }
    public AssignProfileUseCaseImpl(AssignProfileRulesValidator rules, AssignRoleUseCase assignRoleUseCase,
                                    ProfileAssignmentRepository repository, IdentifierGenerator identifiers, TimeProvider time,
                                    AssignmentOutbox outbox, AssignmentEventFactory events) {
        this.rules = Objects.requireNonNull(rules, RequiredArgumentMessages.ASSIGN_PROFILE_RULES_VALIDATOR); this.assignRoleUseCase = Objects.requireNonNull(assignRoleUseCase, RequiredArgumentMessages.ASSIGN_ROLE_USE_CASE);
        this.repository = Objects.requireNonNull(repository, RequiredArgumentMessages.PROFILE_ASSIGNMENT_REPOSITORY); this.identifiers = Objects.requireNonNull(identifiers, RequiredArgumentMessages.IDENTIFIER_GENERATOR); this.time = Objects.requireNonNull(time, RequiredArgumentMessages.TIME_PROVIDER);
        this.outbox = Objects.requireNonNull(outbox); this.events = Objects.requireNonNull(events);
    }

    @Override
    public Mono<ProfileAssignmentResponse> execute(AssignProfileRequest input) {
        return rules.execute(input)
                .flatMap(roleIds -> Flux.fromIterable(roleIds)
                        .concatMap(roleId -> assignRoleUseCase.execute(
                                new AssignRoleRequest(input.tenantId(), input.userId(), input.applicationId(), roleId))
                                .contextWrite(context -> context.put(AssignmentEventContext.SUPPRESS_DIRECT_EVENT, true)))
                        .collectList()
                        .flatMap(generated -> repository.save(ProfileAssignment.grant(
                                new ProfileAssignmentId(identifiers.next()), input.userId(), input.tenantId(),
                                input.applicationId(), input.profileId(), generated.stream().map(assignment -> assignment.id()).collect(Collectors.<AssignmentId>toSet()), time.now()))
                                .flatMap(profile -> outbox == null ? Mono.just(profile) : events.profile("PROFILE_ASSIGNED", profile,
                                        generated.stream().map(assignment -> Assignment.assign(assignment.id(), assignment.userId(), assignment.tenantId(), assignment.applicationId(), assignment.roleId(), assignment.validFrom())).toList(), time.now())
                                        .flatMap(outbox::enqueue).thenReturn(profile))))
                .map(profileAssignment -> new ProfileAssignmentResponse(profileAssignment.id(), profileAssignment.userId(),
                        profileAssignment.tenantId(), profileAssignment.applicationId(), profileAssignment.profileId(),
                        profileAssignment.generatedAssignmentIds(), profileAssignment.validity().validFrom(),
                        profileAssignment.validity().validUntil()));
    }
}
