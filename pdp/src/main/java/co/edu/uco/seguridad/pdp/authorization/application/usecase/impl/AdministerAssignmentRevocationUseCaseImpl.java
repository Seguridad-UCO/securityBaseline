package co.edu.uco.seguridad.pdp.authorization.application.usecase.impl;

import co.edu.uco.seguridad.pdp.assignments.application.usecase.RevokeAssignmentUseCase;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AdministerAssignmentRevocationRequest;
import co.edu.uco.seguridad.pdp.authorization.application.rule.validator.PrincipalMustBeApplicationAdministratorValidator;
import co.edu.uco.seguridad.pdp.authorization.application.usecase.AdministerAssignmentRevocationUseCase;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import reactor.core.publisher.Mono;

import java.util.Objects;

/** Implementación de {@link AdministerAssignmentRevocationUseCase} (HU-018). */
public final class AdministerAssignmentRevocationUseCaseImpl implements AdministerAssignmentRevocationUseCase {

    private final PrincipalMustBeApplicationAdministratorValidator mustBeAdministrator;
    private final RevokeAssignmentUseCase revokeAssignment;

    public AdministerAssignmentRevocationUseCaseImpl(PrincipalMustBeApplicationAdministratorValidator mustBeAdministrator,
            RevokeAssignmentUseCase revokeAssignment) {
        this.mustBeAdministrator = Objects.requireNonNull(mustBeAdministrator,
                RequiredArgumentMessages.PRINCIPAL_MUST_BE_APPLICATION_ADMINISTRATOR_VALIDATOR);
        this.revokeAssignment = Objects.requireNonNull(revokeAssignment, RequiredArgumentMessages.REVOKE_ASSIGNMENT_USE_CASE);
    }

    @Override
    public Mono<Void> execute(AdministerAssignmentRevocationRequest input) {
        return mustBeAdministrator.execute(input.administration())
                .then(Mono.defer(() -> revokeAssignment.execute(input.revocation())));
    }
}
