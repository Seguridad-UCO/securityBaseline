package co.edu.uco.seguridad.pdp.authorization.application.usecase.impl;

import co.edu.uco.seguridad.pdp.applications.application.usecase.RemoveApplicationUseCase;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AdministrationRequest;
import co.edu.uco.seguridad.pdp.authorization.application.rule.validator.PrincipalMustBeApplicationAdministratorValidator;
import co.edu.uco.seguridad.pdp.authorization.application.usecase.AdministerApplicationRemovalUseCase;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * Implementación de {@link AdministerApplicationRemovalUseCase} (HU-015). Pendiente:
 * {@code mustBeAdministrator.execute(input).then(removeApplication.execute(input.applicationId()))}
 * — si el validador rechaza, termina en {@code NotAuthorizedToAdministerException} sin llegar a
 * invocar {@code RemoveApplicationUseCase}.
 */
public final class AdministerApplicationRemovalUseCaseImpl implements AdministerApplicationRemovalUseCase {

    private final PrincipalMustBeApplicationAdministratorValidator mustBeAdministrator;
    private final RemoveApplicationUseCase removeApplication;

    public AdministerApplicationRemovalUseCaseImpl(PrincipalMustBeApplicationAdministratorValidator mustBeAdministrator,
            RemoveApplicationUseCase removeApplication) {
        this.mustBeAdministrator = Objects.requireNonNull(mustBeAdministrator,
                RequiredArgumentMessages.PRINCIPAL_MUST_BE_APPLICATION_ADMINISTRATOR_VALIDATOR);
        this.removeApplication = Objects.requireNonNull(removeApplication, RequiredArgumentMessages.REMOVE_APPLICATION_USE_CASE);
    }

    @Override
    public Mono<Void> execute(AdministrationRequest input) {
        return mustBeAdministrator.execute(input)
                .then(Mono.defer(() -> removeApplication.execute(input.applicationId())));
    }
}
