package co.edu.uco.seguridad.pdp.authorization.application.usecase.impl;

import co.edu.uco.seguridad.pdp.assignments.application.primaryport.response.AssignmentResponse;
import co.edu.uco.seguridad.pdp.assignments.application.usecase.AssignRoleUseCase;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AdministerAssignmentCreationRequest;
import co.edu.uco.seguridad.pdp.authorization.application.rule.validator.PrincipalMustBeApplicationAdministratorValidator;
import co.edu.uco.seguridad.pdp.authorization.application.usecase.AdministerAssignmentCreationUseCase;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import reactor.core.publisher.Mono;

import java.util.Objects;

/** Implementación de {@link AdministerAssignmentCreationUseCase} (HU-018). */
public final class AdministerAssignmentCreationUseCaseImpl implements AdministerAssignmentCreationUseCase {

    private final PrincipalMustBeApplicationAdministratorValidator mustBeAdministrator;
    private final AssignRoleUseCase assignRole;

    public AdministerAssignmentCreationUseCaseImpl(PrincipalMustBeApplicationAdministratorValidator mustBeAdministrator,
            AssignRoleUseCase assignRole) {
        this.mustBeAdministrator = Objects.requireNonNull(mustBeAdministrator,
                RequiredArgumentMessages.PRINCIPAL_MUST_BE_APPLICATION_ADMINISTRATOR_VALIDATOR);
        this.assignRole = Objects.requireNonNull(assignRole, RequiredArgumentMessages.ASSIGN_ROLE_USE_CASE);
    }

    @Override
    public Mono<AssignmentResponse> execute(AdministerAssignmentCreationRequest input) {
        return mustBeAdministrator.execute(input.administration())
                .then(Mono.defer(() -> assignRole.execute(input.assignment())));
    }
}
