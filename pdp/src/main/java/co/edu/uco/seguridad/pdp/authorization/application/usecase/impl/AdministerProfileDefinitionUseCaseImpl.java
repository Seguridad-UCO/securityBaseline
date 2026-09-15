package co.edu.uco.seguridad.pdp.authorization.application.usecase.impl;

import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AdministerProfileDefinitionRequest;
import co.edu.uco.seguridad.pdp.authorization.application.rule.validator.PrincipalMustBeApplicationAdministratorValidator;
import co.edu.uco.seguridad.pdp.authorization.application.usecase.AdministerProfileDefinitionUseCase;
import co.edu.uco.seguridad.pdp.profiles.application.primaryport.response.ProfileResponse;
import co.edu.uco.seguridad.pdp.profiles.application.usecase.DefineProfileUseCase;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import reactor.core.publisher.Mono;

import java.util.Objects;

/** Implementación de {@link AdministerProfileDefinitionUseCase} (HU-019). */
public final class AdministerProfileDefinitionUseCaseImpl implements AdministerProfileDefinitionUseCase {

    private final PrincipalMustBeApplicationAdministratorValidator mustBeAdministrator;
    private final DefineProfileUseCase defineProfile;

    public AdministerProfileDefinitionUseCaseImpl(PrincipalMustBeApplicationAdministratorValidator mustBeAdministrator,
            DefineProfileUseCase defineProfile) {
        this.mustBeAdministrator = Objects.requireNonNull(mustBeAdministrator,
                RequiredArgumentMessages.PRINCIPAL_MUST_BE_APPLICATION_ADMINISTRATOR_VALIDATOR);
        this.defineProfile = Objects.requireNonNull(defineProfile, RequiredArgumentMessages.DEFINE_PROFILE_USE_CASE);
    }

    @Override
    public Mono<ProfileResponse> execute(AdministerProfileDefinitionRequest input) {
        Mono<Void> gate = input.administration().map(mustBeAdministrator::execute).orElseGet(Mono::empty);
        return gate.then(Mono.defer(() -> defineProfile.execute(input.profile())));
    }
}
