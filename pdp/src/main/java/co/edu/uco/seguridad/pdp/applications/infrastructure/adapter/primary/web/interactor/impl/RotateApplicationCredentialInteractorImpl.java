package co.edu.uco.seguridad.pdp.applications.infrastructure.adapter.primary.web.interactor.impl;

import co.edu.uco.seguridad.pdp.applications.application.usecase.RotateApplicationCredentialUseCase;
import co.edu.uco.seguridad.pdp.applications.infrastructure.adapter.primary.web.dto.request.raw.RotateApplicationCredentialRawRequest;
import co.edu.uco.seguridad.pdp.applications.infrastructure.adapter.primary.web.dto.response.ApplicationRegisteredWebResponse;
import co.edu.uco.seguridad.pdp.applications.infrastructure.adapter.primary.web.interactor.RotateApplicationCredentialInteractor;
import co.edu.uco.seguridad.pdp.applications.infrastructure.adapter.primary.web.mapper.ApplicationResponseMapper;
import co.edu.uco.seguridad.pdp.applications.infrastructure.adapter.primary.web.mapper.RotateApplicationCredentialRequestMapper;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import co.edu.uco.seguridad.shared.security.SecurityContext;
import reactor.core.publisher.Mono;

import java.util.Objects;

public final class RotateApplicationCredentialInteractorImpl implements RotateApplicationCredentialInteractor {

    private final RotateApplicationCredentialUseCase useCase;

    public RotateApplicationCredentialInteractorImpl(RotateApplicationCredentialUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, RequiredArgumentMessages.ROTATE_APPLICATION_CREDENTIAL_USE_CASE);
    }

    @Override
    public Mono<ApplicationRegisteredWebResponse> execute(RotateApplicationCredentialRawRequest raw) {
        return SecurityContext.currentPrincipal()
                .map(principal -> RotateApplicationCredentialRequestMapper.toRequest(raw, principal.tenantId()))
                .flatMap(useCase::execute)
                .map(ApplicationResponseMapper::toRegisteredResponse);
    }
}
