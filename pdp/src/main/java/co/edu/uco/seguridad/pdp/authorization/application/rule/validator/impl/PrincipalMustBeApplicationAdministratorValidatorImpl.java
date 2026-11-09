package co.edu.uco.seguridad.pdp.authorization.application.rule.validator.impl;

import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AdministrationRequest;
import co.edu.uco.seguridad.pdp.authorization.application.rule.validator.PrincipalMustBeApplicationAdministratorValidator;
import co.edu.uco.seguridad.pdp.authorization.application.usecase.AuthorizeAdministrationUseCase;
import co.edu.uco.seguridad.pdp.authorization.domain.exception.NotAuthorizedToAdministerException;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * Implementación de {@link PrincipalMustBeApplicationAdministratorValidator} (HU-009). Pendiente:
 * delegar en {@link AuthorizeAdministrationUseCase} y, si la decisión no permite, terminar en
 * {@code NotAuthorizedToAdministerException(input.tenantId(), input.applicationId())} — igual de
 * cerrado ante {@code DENY} que ante {@code INDETERMINATE}.
 */
public final class PrincipalMustBeApplicationAdministratorValidatorImpl
        implements PrincipalMustBeApplicationAdministratorValidator {

    private final AuthorizeAdministrationUseCase useCase;

    public PrincipalMustBeApplicationAdministratorValidatorImpl(AuthorizeAdministrationUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, RequiredArgumentMessages.AUTHORIZE_ADMINISTRATION_USE_CASE);
    }

    @Override
    public Mono<Void> execute(AdministrationRequest input) {
        return useCase.execute(input)
                .flatMap(decision -> decision.permits()
                        ? Mono.empty()
                        : Mono.error(new NotAuthorizedToAdministerException(input.tenantId(), input.applicationId())));
    }
}
