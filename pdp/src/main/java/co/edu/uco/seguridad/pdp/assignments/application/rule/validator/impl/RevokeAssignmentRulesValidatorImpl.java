package co.edu.uco.seguridad.pdp.assignments.application.rule.validator.impl;

import co.edu.uco.seguridad.pdp.assignments.application.primaryport.request.RevokeAssignmentRequest;
import co.edu.uco.seguridad.pdp.assignments.application.rule.validator.RevokeAssignmentRulesValidator;
import co.edu.uco.seguridad.pdp.assignments.application.secondaryport.repository.AssignmentRepository;
import co.edu.uco.seguridad.pdp.assignments.domain.Assignment;
import co.edu.uco.seguridad.pdp.assignments.domain.rule.AssignmentMustExistForTenantRule;
import co.edu.uco.seguridad.pdp.assignments.domain.rule.model.AssignmentExistence;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import reactor.core.publisher.Mono;

import java.util.Objects;

public final class RevokeAssignmentRulesValidatorImpl implements RevokeAssignmentRulesValidator {

    private final AssignmentRepository repository;
    private final AssignmentMustExistForTenantRule mustExist;

    public RevokeAssignmentRulesValidatorImpl(AssignmentRepository repository, AssignmentMustExistForTenantRule mustExist) {
        this.repository = Objects.requireNonNull(repository, RequiredArgumentMessages.ASSIGNMENT_REPOSITORY);
        this.mustExist = Objects.requireNonNull(mustExist, RequiredArgumentMessages.ASSIGNMENT_EXISTS_RULE);
    }

    @Override
    public Mono<Assignment> execute(RevokeAssignmentRequest input) {
        return repository.findByIdForTenant(input.assignmentId(), input.tenantId())
                .switchIfEmpty(Mono.fromRunnable(
                        () -> mustExist.execute(new AssignmentExistence(input.assignmentId(), input.tenantId(), false))));
    }
}
