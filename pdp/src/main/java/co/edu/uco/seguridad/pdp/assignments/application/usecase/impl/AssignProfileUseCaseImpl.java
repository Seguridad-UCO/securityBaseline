package co.edu.uco.seguridad.pdp.assignments.application.usecase.impl;

import co.edu.uco.seguridad.pdp.assignments.application.primaryport.request.AssignProfileRequest;
import co.edu.uco.seguridad.pdp.assignments.application.primaryport.request.AssignRoleRequest;
import co.edu.uco.seguridad.pdp.assignments.application.primaryport.response.ProfileAssignmentResponse;
import co.edu.uco.seguridad.pdp.assignments.application.rule.validator.AssignProfileRulesValidator;
import co.edu.uco.seguridad.pdp.assignments.application.secondaryport.repository.ProfileAssignmentRepository;
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

    public AssignProfileUseCaseImpl(AssignProfileRulesValidator rules, AssignRoleUseCase assignRoleUseCase,
                                    ProfileAssignmentRepository repository, IdentifierGenerator identifiers, TimeProvider time) {
        this.rules = Objects.requireNonNull(rules, RequiredArgumentMessages.ASSIGN_PROFILE_RULES_VALIDATOR);
        this.assignRoleUseCase = Objects.requireNonNull(assignRoleUseCase, RequiredArgumentMessages.ASSIGN_ROLE_USE_CASE);
        this.repository = Objects.requireNonNull(repository, RequiredArgumentMessages.PROFILE_ASSIGNMENT_REPOSITORY);
        this.identifiers = Objects.requireNonNull(identifiers, RequiredArgumentMessages.IDENTIFIER_GENERATOR);
        this.time = Objects.requireNonNull(time, RequiredArgumentMessages.TIME_PROVIDER);
    }

    @Override
    public Mono<ProfileAssignmentResponse> execute(AssignProfileRequest input) {
        return rules.execute(input)
                .flatMap(roleIds -> Flux.fromIterable(roleIds)
                        .concatMap(roleId -> assignRoleUseCase.execute(
                                new AssignRoleRequest(input.tenantId(), input.userId(), input.applicationId(), roleId)))
                        .map(assignment -> assignment.id())
                        .collect(Collectors.<AssignmentId>toSet())
                        .flatMap(generatedIds -> repository.save(ProfileAssignment.grant(
                                new ProfileAssignmentId(identifiers.next()), input.userId(), input.tenantId(),
                                input.applicationId(), input.profileId(), generatedIds, time.now()))))
                .map(profileAssignment -> new ProfileAssignmentResponse(profileAssignment.id(), profileAssignment.userId(),
                        profileAssignment.tenantId(), profileAssignment.applicationId(), profileAssignment.profileId(),
                        profileAssignment.generatedAssignmentIds(), profileAssignment.validity().validFrom(),
                        profileAssignment.validity().validUntil()));
    }
}
