package co.edu.uco.seguridad.pdp.assignments.application.usecase.impl;

import co.edu.uco.seguridad.pdp.assignments.application.primaryport.request.RevokeAssignmentRequest;
import co.edu.uco.seguridad.pdp.assignments.application.primaryport.request.RevokeProfileAssignmentRequest;
import co.edu.uco.seguridad.pdp.assignments.application.rule.validator.RevokeProfileAssignmentRulesValidator;
import co.edu.uco.seguridad.pdp.assignments.application.secondaryport.repository.ProfileAssignmentRepository;
import co.edu.uco.seguridad.pdp.assignments.application.usecase.RevokeAssignmentUseCase;
import co.edu.uco.seguridad.pdp.assignments.application.usecase.RevokeProfileAssignmentUseCase;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import co.edu.uco.seguridad.shared.port.TimeProvider;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Objects;

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

    public RevokeProfileAssignmentUseCaseImpl(RevokeProfileAssignmentRulesValidator rules,
                                              RevokeAssignmentUseCase revokeAssignmentUseCase, ProfileAssignmentRepository repository, TimeProvider time) {
        this.rules = Objects.requireNonNull(rules, RequiredArgumentMessages.REVOKE_PROFILE_ASSIGNMENT_RULES_VALIDATOR);
        this.revokeAssignmentUseCase = Objects.requireNonNull(revokeAssignmentUseCase,
                RequiredArgumentMessages.REVOKE_ASSIGNMENT_USE_CASE);
        this.repository = Objects.requireNonNull(repository, RequiredArgumentMessages.PROFILE_ASSIGNMENT_REPOSITORY);
        this.time = Objects.requireNonNull(time, RequiredArgumentMessages.TIME_PROVIDER);
    }

    @Override
    public Mono<Void> execute(RevokeProfileAssignmentRequest input) {
        return rules.execute(input)
                .flatMap(profileAssignment -> Flux.fromIterable(profileAssignment.generatedAssignmentIds())
                        .concatMap(assignmentId -> revokeAssignmentUseCase.execute(
                                new RevokeAssignmentRequest(assignmentId, input.tenantId())))
                        .then(Mono.defer(() -> repository.save(profileAssignment.revoke(time.now())))))
                .then();
    }
}
