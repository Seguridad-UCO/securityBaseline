package co.edu.uco.seguridad.pdp.authorization.application.usecase.impl;

import co.edu.uco.seguridad.pdp.applications.application.primaryport.response.ApplicationRegistrationResponse;
import co.edu.uco.seguridad.pdp.applications.application.usecase.RotateApplicationCredentialUseCase;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AdministrationRequest;
import co.edu.uco.seguridad.pdp.authorization.application.rule.validator.PrincipalMustBeApplicationAdministratorValidator;
import co.edu.uco.seguridad.pdp.authorization.application.usecase.AdministerApplicationCredentialRotationUseCase;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * Implementación de {@link AdministerApplicationCredentialRotationUseCase} (HU-015). Pendiente:
 * {@code mustBeAdministrator.execute(input).then(rotateCredential.execute(new RotateApplicationCredentialRequest(input.tenantId(), input.applicationId())))}.
 */
public final class AdministerApplicationCredentialRotationUseCaseImpl
        implements AdministerApplicationCredentialRotationUseCase {

    private final PrincipalMustBeApplicationAdministratorValidator mustBeAdministrator;
    private final RotateApplicationCredentialUseCase rotateCredential;

    public AdministerApplicationCredentialRotationUseCaseImpl(
            PrincipalMustBeApplicationAdministratorValidator mustBeAdministrator,
            RotateApplicationCredentialUseCase rotateCredential) {
        this.mustBeAdministrator = Objects.requireNonNull(mustBeAdministrator,
                RequiredArgumentMessages.PRINCIPAL_MUST_BE_APPLICATION_ADMINISTRATOR_VALIDATOR);
        this.rotateCredential = Objects.requireNonNull(rotateCredential,
                RequiredArgumentMessages.ROTATE_APPLICATION_CREDENTIAL_USE_CASE);
    }

    @Override
    public Mono<ApplicationRegistrationResponse> execute(AdministrationRequest input) {
        throw new UnsupportedOperationException("pendiente: HU-015");
    }
}
