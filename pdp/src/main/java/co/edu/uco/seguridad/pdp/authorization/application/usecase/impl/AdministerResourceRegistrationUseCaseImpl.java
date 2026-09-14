package co.edu.uco.seguridad.pdp.authorization.application.usecase.impl;

import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AdministerResourceRegistrationRequest;
import co.edu.uco.seguridad.pdp.authorization.application.rule.validator.PrincipalMustBeApplicationAdministratorValidator;
import co.edu.uco.seguridad.pdp.authorization.application.usecase.AdministerResourceRegistrationUseCase;
import co.edu.uco.seguridad.pdp.resources.application.primaryport.response.RegisteredProtectedResourceResponse;
import co.edu.uco.seguridad.pdp.resources.application.usecase.RegisterProtectedResourceUseCase;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * Implementación de {@link AdministerResourceRegistrationUseCase} (HU-017). El gate siempre se
 * evalúa — sin {@code Optional}, a diferencia de {@code AdministerRoleDefinitionUseCaseImpl}: un
 * recurso protegido siempre pertenece a una aplicación.
 */
public final class AdministerResourceRegistrationUseCaseImpl implements AdministerResourceRegistrationUseCase {

    private final PrincipalMustBeApplicationAdministratorValidator mustBeAdministrator;
    private final RegisterProtectedResourceUseCase registerResource;

    public AdministerResourceRegistrationUseCaseImpl(PrincipalMustBeApplicationAdministratorValidator mustBeAdministrator,
            RegisterProtectedResourceUseCase registerResource) {
        this.mustBeAdministrator = Objects.requireNonNull(mustBeAdministrator,
                RequiredArgumentMessages.PRINCIPAL_MUST_BE_APPLICATION_ADMINISTRATOR_VALIDATOR);
        this.registerResource = Objects.requireNonNull(registerResource, RequiredArgumentMessages.REGISTER_USE_CASE);
    }

    @Override
    public Mono<RegisteredProtectedResourceResponse> execute(AdministerResourceRegistrationRequest input) {
        return mustBeAdministrator.execute(input.administration())
                .then(Mono.defer(() -> registerResource.execute(input.resource())));
    }
}
