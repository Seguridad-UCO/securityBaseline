package co.edu.uco.seguridad.pdp.assignments.application.rule.validator.impl;

import co.edu.uco.seguridad.pdp.assignments.application.primaryport.request.ProfileAssignmentOwnershipQuery;
import co.edu.uco.seguridad.pdp.assignments.application.rule.validator.ProfileAssignmentApplicationLookupValidator;
import co.edu.uco.seguridad.pdp.assignments.application.secondaryport.repository.ProfileAssignmentRepository;
import co.edu.uco.seguridad.pdp.assignments.domain.rule.ProfileAssignmentMustExistForTenantRule;
import co.edu.uco.seguridad.pdp.assignments.domain.rule.model.ProfileAssignmentExistence;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import reactor.core.publisher.Mono;

import java.util.Objects;

/** Implementación de {@link ProfileAssignmentApplicationLookupValidator} (HU-019). */
public final class ProfileAssignmentApplicationLookupValidatorImpl implements ProfileAssignmentApplicationLookupValidator {

    private final ProfileAssignmentRepository repository;
    private final ProfileAssignmentMustExistForTenantRule mustExist;

    public ProfileAssignmentApplicationLookupValidatorImpl(ProfileAssignmentRepository repository,
            ProfileAssignmentMustExistForTenantRule mustExist) {
        this.repository = Objects.requireNonNull(repository, RequiredArgumentMessages.PROFILE_ASSIGNMENT_REPOSITORY);
        this.mustExist = Objects.requireNonNull(mustExist, RequiredArgumentMessages.PROFILE_ASSIGNMENT_EXISTS_RULE);
    }

    @Override
    public Mono<ApplicationId> execute(ProfileAssignmentOwnershipQuery input) {
        return repository.findByIdForTenant(input.profileAssignmentId(), input.tenantId())
                .doOnNext(assignment -> mustExist.execute(
                        new ProfileAssignmentExistence(input.profileAssignmentId(), input.tenantId(), true)))
                .switchIfEmpty(Mono.fromRunnable(() -> mustExist.execute(
                        new ProfileAssignmentExistence(input.profileAssignmentId(), input.tenantId(), false))))
                .map(assignment -> assignment.applicationId());
    }
}
