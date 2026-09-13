package co.edu.uco.seguridad.pdp.assignments.application.rule.validator.impl;

import co.edu.uco.seguridad.pdp.assignments.application.primaryport.request.RevokeProfileAssignmentRequest;
import co.edu.uco.seguridad.pdp.assignments.application.rule.validator.RevokeProfileAssignmentRulesValidator;
import co.edu.uco.seguridad.pdp.assignments.application.secondaryport.repository.ProfileAssignmentRepository;
import co.edu.uco.seguridad.pdp.assignments.domain.ProfileAssignment;
import co.edu.uco.seguridad.pdp.assignments.domain.rule.ProfileAssignmentMustExistForTenantRule;
import co.edu.uco.seguridad.pdp.assignments.domain.rule.model.ProfileAssignmentExistence;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import reactor.core.publisher.Mono;

import java.util.Objects;

public final class RevokeProfileAssignmentRulesValidatorImpl implements RevokeProfileAssignmentRulesValidator {

    private final ProfileAssignmentRepository repository;
    private final ProfileAssignmentMustExistForTenantRule mustExist;

    public RevokeProfileAssignmentRulesValidatorImpl(ProfileAssignmentRepository repository,
            ProfileAssignmentMustExistForTenantRule mustExist) {
        this.repository = Objects.requireNonNull(repository, RequiredArgumentMessages.PROFILE_ASSIGNMENT_REPOSITORY);
        this.mustExist = Objects.requireNonNull(mustExist, RequiredArgumentMessages.PROFILE_ASSIGNMENT_EXISTS_RULE);
    }

    @Override
    public Mono<ProfileAssignment> execute(RevokeProfileAssignmentRequest input) {
        return repository.findByIdForTenant(input.profileAssignmentId(), input.tenantId())
                .switchIfEmpty(Mono.fromRunnable(() -> mustExist.execute(
                        new ProfileAssignmentExistence(input.profileAssignmentId(), input.tenantId(), false))));
    }
}
