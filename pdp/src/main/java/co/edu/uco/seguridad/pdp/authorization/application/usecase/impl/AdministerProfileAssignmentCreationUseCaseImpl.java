package co.edu.uco.seguridad.pdp.authorization.application.usecase.impl;

import co.edu.uco.seguridad.pdp.assignments.application.primaryport.response.ProfileAssignmentResponse;
import co.edu.uco.seguridad.pdp.assignments.application.usecase.AssignProfileUseCase;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AdministerProfileAssignmentCreationRequest;
import co.edu.uco.seguridad.pdp.authorization.application.rule.validator.PrincipalMustBeApplicationAdministratorValidator;
import co.edu.uco.seguridad.pdp.authorization.application.usecase.AdministerProfileAssignmentCreationUseCase;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import reactor.core.publisher.Mono;

import java.util.Objects;

/** Implementación de {@link AdministerProfileAssignmentCreationUseCase} (HU-019). */
public final class AdministerProfileAssignmentCreationUseCaseImpl implements AdministerProfileAssignmentCreationUseCase {

    private final PrincipalMustBeApplicationAdministratorValidator mustBeAdministrator;
    private final AssignProfileUseCase assignProfile;

    public AdministerProfileAssignmentCreationUseCaseImpl(PrincipalMustBeApplicationAdministratorValidator mustBeAdministrator,
            AssignProfileUseCase assignProfile) {
        this.mustBeAdministrator = Objects.requireNonNull(mustBeAdministrator,
                RequiredArgumentMessages.PRINCIPAL_MUST_BE_APPLICATION_ADMINISTRATOR_VALIDATOR);
        this.assignProfile = Objects.requireNonNull(assignProfile, RequiredArgumentMessages.ASSIGN_PROFILE_USE_CASE);
    }

    @Override
    public Mono<ProfileAssignmentResponse> execute(AdministerProfileAssignmentCreationRequest input) {
        return mustBeAdministrator.execute(input.administration())
                .then(Mono.defer(() -> assignProfile.execute(input.assignment())));
    }
}
