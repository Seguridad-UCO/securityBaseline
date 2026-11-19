package co.edu.uco.seguridad.pdp.assignments.application.usecase.impl;

import co.edu.uco.seguridad.pdp.assignments.application.primaryport.request.RevokeAssignmentRequest;
import co.edu.uco.seguridad.pdp.assignments.application.primaryport.request.RevokeProfileAssignmentRequest;
import co.edu.uco.seguridad.pdp.assignments.application.rule.validator.RevokeProfileAssignmentRulesValidator;
import co.edu.uco.seguridad.pdp.assignments.application.secondaryport.repository.ProfileAssignmentRepository;
import co.edu.uco.seguridad.pdp.assignments.application.secondaryport.repository.AssignmentRepository;
import co.edu.uco.seguridad.pdp.assignments.application.secondaryport.event.AssignmentOutbox;
import co.edu.uco.seguridad.pdp.assignments.application.event.AssignmentEventFactory;
import co.edu.uco.seguridad.pdp.assignments.domain.Assignment;
import co.edu.uco.seguridad.pdp.assignments.application.usecase.RevokeAssignmentUseCase;
import co.edu.uco.seguridad.pdp.assignments.application.usecase.RevokeProfileAssignmentUseCase;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import co.edu.uco.seguridad.shared.port.TimeProvider;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Objects;
import co.edu.uco.seguridad.pdp.assignments.application.event.AssignmentEventContext;

/**
 * Revoca en cascada cada {@code Assignment} que el perfil generó, reutilizando
 * {@code RevokeAssignmentUseCase} tal cual, y luego se revoca a sí mismo (decisión confirmada, ver
 * PLAN-HU-011.md §11).
 */
public final class RevokeProfileAssignmentUseCaseImpl implements RevokeProfileAssignmentUseCase {

    private final RevokeProfileAssignmentRulesValidator rules;
    private final RevokeAssignmentUseCase revokeAssignmentUseCase;
    private final ProfileAssignmentRepository repository;
    private final TimeProvider time;
    private final AssignmentRepository assignments; private final AssignmentOutbox outbox; private final AssignmentEventFactory events;

    public RevokeProfileAssignmentUseCaseImpl(RevokeProfileAssignmentRulesValidator rules,
                                              RevokeAssignmentUseCase revokeAssignmentUseCase, ProfileAssignmentRepository repository, TimeProvider time) {
        this.rules = Objects.requireNonNull(rules, RequiredArgumentMessages.REVOKE_PROFILE_ASSIGNMENT_RULES_VALIDATOR);
        this.revokeAssignmentUseCase = Objects.requireNonNull(revokeAssignmentUseCase,
                RequiredArgumentMessages.REVOKE_ASSIGNMENT_USE_CASE);
        this.repository = Objects.requireNonNull(repository, RequiredArgumentMessages.PROFILE_ASSIGNMENT_REPOSITORY);
        this.time = Objects.requireNonNull(time, RequiredArgumentMessages.TIME_PROVIDER);
        this.assignments = null; this.outbox = null; this.events = null;
    }
    public RevokeProfileAssignmentUseCaseImpl(RevokeProfileAssignmentRulesValidator rules, RevokeAssignmentUseCase revokeAssignmentUseCase,
                                              ProfileAssignmentRepository repository, TimeProvider time, AssignmentRepository assignments,
                                              AssignmentOutbox outbox, AssignmentEventFactory events) {
        this.rules = Objects.requireNonNull(rules, RequiredArgumentMessages.REVOKE_PROFILE_ASSIGNMENT_RULES_VALIDATOR); this.revokeAssignmentUseCase = Objects.requireNonNull(revokeAssignmentUseCase, RequiredArgumentMessages.REVOKE_ASSIGNMENT_USE_CASE);
        this.repository = Objects.requireNonNull(repository, RequiredArgumentMessages.PROFILE_ASSIGNMENT_REPOSITORY); this.time = Objects.requireNonNull(time, RequiredArgumentMessages.TIME_PROVIDER);
        this.assignments = Objects.requireNonNull(assignments); this.outbox = Objects.requireNonNull(outbox); this.events = Objects.requireNonNull(events);
    }

    @Override
    public Mono<Void> execute(RevokeProfileAssignmentRequest input) {
        return rules.execute(input)
                .flatMap(profileAssignment -> (assignments == null ? Mono.just(java.util.List.<Assignment>of()) : Flux.fromIterable(profileAssignment.generatedAssignmentIds()).concatMap(id -> assignments.findByIdForTenant(id, input.tenantId())).collectList())
                        .flatMap(materialized -> Flux.fromIterable(profileAssignment.generatedAssignmentIds())
                        .concatMap(assignmentId -> revokeAssignmentUseCase.execute(
                                new RevokeAssignmentRequest(assignmentId, input.tenantId()))
                                .contextWrite(context -> context.put(AssignmentEventContext.SUPPRESS_DIRECT_EVENT, true)))
                        .then(Mono.defer(() -> repository.save(profileAssignment.revoke(time.now()))))
                        .flatMap(profile -> outbox == null ? Mono.just(profile) : events.profile("PROFILE_REVOKED", profile, materialized, time.now()).flatMap(outbox::enqueue).thenReturn(profile))))
                .then();
    }
}
