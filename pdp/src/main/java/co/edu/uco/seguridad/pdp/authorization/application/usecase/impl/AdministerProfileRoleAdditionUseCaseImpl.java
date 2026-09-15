package co.edu.uco.seguridad.pdp.authorization.application.usecase.impl;

import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AdministerProfileRoleAdditionRequest;
import co.edu.uco.seguridad.pdp.authorization.application.rule.validator.PrincipalMustBeApplicationAdministratorValidator;
import co.edu.uco.seguridad.pdp.authorization.application.usecase.AdministerProfileRoleAdditionUseCase;
import co.edu.uco.seguridad.pdp.profiles.application.primaryport.response.ProfileResponse;
import co.edu.uco.seguridad.pdp.profiles.application.usecase.AddRoleToProfileUseCase;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import reactor.core.publisher.Mono;

import java.util.Objects;

/** Implementación de {@link AdministerProfileRoleAdditionUseCase} (HU-019). */
public final class AdministerProfileRoleAdditionUseCaseImpl implements AdministerProfileRoleAdditionUseCase {

    private final PrincipalMustBeApplicationAdministratorValidator mustBeAdministrator;
    private final AddRoleToProfileUseCase addRoleToProfile;

    public AdministerProfileRoleAdditionUseCaseImpl(PrincipalMustBeApplicationAdministratorValidator mustBeAdministrator,
            AddRoleToProfileUseCase addRoleToProfile) {
        this.mustBeAdministrator = Objects.requireNonNull(mustBeAdministrator,
                RequiredArgumentMessages.PRINCIPAL_MUST_BE_APPLICATION_ADMINISTRATOR_VALIDATOR);
        this.addRoleToProfile = Objects.requireNonNull(addRoleToProfile, RequiredArgumentMessages.ADD_ROLE_TO_PROFILE_USE_CASE);
    }

    @Override
    public Mono<ProfileResponse> execute(AdministerProfileRoleAdditionRequest input) {
        Mono<Void> gate = input.administration().map(mustBeAdministrator::execute).orElseGet(Mono::empty);
        return gate.then(Mono.defer(() -> addRoleToProfile.execute(input.addition())));
    }
}
