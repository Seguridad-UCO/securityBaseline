package co.edu.uco.seguridad.pdp.assignments.application.rule.validator.impl;

import co.edu.uco.seguridad.pdp.assignments.application.primaryport.request.AssignmentOwnershipQuery;
import co.edu.uco.seguridad.pdp.assignments.application.rule.validator.AssignmentApplicationLookupValidator;
import co.edu.uco.seguridad.pdp.assignments.application.secondaryport.repository.AssignmentRepository;
import co.edu.uco.seguridad.pdp.assignments.domain.rule.AssignmentMustExistForTenantRule;
import co.edu.uco.seguridad.pdp.assignments.domain.rule.model.AssignmentExistence;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * Implementación de {@link AssignmentApplicationLookupValidator} (HU-018).
 */
public final class AssignmentApplicationLookupValidatorImpl implements AssignmentApplicationLookupValidator {

    private final AssignmentRepository repository;
    private final AssignmentMustExistForTenantRule mustExist;

    public AssignmentApplicationLookupValidatorImpl(AssignmentRepository repository, AssignmentMustExistForTenantRule mustExist) {
        this.repository = Objects.requireNonNull(repository, RequiredArgumentMessages.ASSIGNMENT_REPOSITORY);
        this.mustExist = Objects.requireNonNull(mustExist, RequiredArgumentMessages.ASSIGNMENT_EXISTS_RULE);
    }

    @Override
    public Mono<ApplicationId> execute(AssignmentOwnershipQuery input) {
        return repository.findByIdForTenant(input.assignmentId(), input.tenantId())
                .doOnNext(assignment -> mustExist.execute(new AssignmentExistence(input.assignmentId(), input.tenantId(), true)))
                .switchIfEmpty(Mono.fromRunnable(
                        () -> mustExist.execute(new AssignmentExistence(input.assignmentId(), input.tenantId(), false))))
                .map(assignment -> assignment.applicationId());
    }
}
