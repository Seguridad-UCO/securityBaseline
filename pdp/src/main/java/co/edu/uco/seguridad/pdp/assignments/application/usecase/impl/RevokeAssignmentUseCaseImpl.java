package co.edu.uco.seguridad.pdp.assignments.application.usecase.impl;

import co.edu.uco.seguridad.pdp.assignments.application.primaryport.request.RevokeAssignmentRequest;
import co.edu.uco.seguridad.pdp.assignments.application.rule.validator.RevokeAssignmentRulesValidator;
import co.edu.uco.seguridad.pdp.assignments.application.secondaryport.repository.AssignmentRepository;
import co.edu.uco.seguridad.pdp.assignments.application.usecase.RevokeAssignmentUseCase;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import co.edu.uco.seguridad.shared.port.TimeProvider;
import reactor.core.publisher.Mono;

import java.util.Objects;

/** Transforma lo que el validador ya encontró (Assignment.revoke) y lo guarda. No decide nada. */
public final class RevokeAssignmentUseCaseImpl implements RevokeAssignmentUseCase {

    private final RevokeAssignmentRulesValidator rules;
    private final AssignmentRepository repository;
    private final TimeProvider time;

    public RevokeAssignmentUseCaseImpl(RevokeAssignmentRulesValidator rules, AssignmentRepository repository, TimeProvider time) {
        this.rules = Objects.requireNonNull(rules, RequiredArgumentMessages.REVOKE_ASSIGNMENT_RULES_VALIDATOR);
        this.repository = Objects.requireNonNull(repository, RequiredArgumentMessages.ASSIGNMENT_REPOSITORY);
        this.time = Objects.requireNonNull(time, RequiredArgumentMessages.TIME_PROVIDER);
    }

    @Override
    public Mono<Void> execute(RevokeAssignmentRequest input) {
        return rules.execute(input)
                .map(assignment -> assignment.revoke(time.now()))
                .flatMap(repository::save)
                .then();
    }
}
