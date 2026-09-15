package co.edu.uco.seguridad.pdp.authorization.application.usecase.impl;

import co.edu.uco.seguridad.pdp.assignments.application.usecase.RevokeProfileAssignmentUseCase;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AdministerProfileAssignmentRevocationRequest;
import co.edu.uco.seguridad.pdp.authorization.application.rule.validator.PrincipalMustBeApplicationAdministratorValidator;
import co.edu.uco.seguridad.pdp.authorization.application.usecase.AdministerProfileAssignmentRevocationUseCase;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import reactor.core.publisher.Mono;

import java.util.Objects;

/** Implementación de {@link AdministerProfileAssignmentRevocationUseCase} (HU-019). */
public final class AdministerProfileAssignmentRevocationUseCaseImpl implements AdministerProfileAssignmentRevocationUseCase {

    private final PrincipalMustBeApplicationAdministratorValidator mustBeAdministrator;
    private final RevokeProfileAssignmentUseCase revokeProfileAssignment;

    public AdministerProfileAssignmentRevocationUseCaseImpl(
            PrincipalMustBeApplicationAdministratorValidator mustBeAdministrator,
            RevokeProfileAssignmentUseCase revokeProfileAssignment) {
        this.mustBeAdministrator = Objects.requireNonNull(mustBeAdministrator,
                RequiredArgumentMessages.PRINCIPAL_MUST_BE_APPLICATION_ADMINISTRATOR_VALIDATOR);
        this.revokeProfileAssignment = Objects.requireNonNull(revokeProfileAssignment,
                RequiredArgumentMessages.REVOKE_PROFILE_ASSIGNMENT_USE_CASE);
    }

    @Override
    public Mono<Void> execute(AdministerProfileAssignmentRevocationRequest input) {
        return mustBeAdministrator.execute(input.administration())
                .then(Mono.defer(() -> revokeProfileAssignment.execute(input.revocation())));
    }
}
