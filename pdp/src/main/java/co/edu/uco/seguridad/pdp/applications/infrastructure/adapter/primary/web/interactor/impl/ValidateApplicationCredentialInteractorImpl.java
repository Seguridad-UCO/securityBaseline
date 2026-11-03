package co.edu.uco.seguridad.pdp.applications.infrastructure.adapter.primary.web.interactor.impl;

import co.edu.uco.seguridad.pdp.applications.application.usecase.ValidateApplicationCredentialUseCase;
import co.edu.uco.seguridad.pdp.applications.infrastructure.adapter.primary.web.dto.request.raw.ValidateApplicationCredentialRawRequest;
import co.edu.uco.seguridad.pdp.applications.infrastructure.adapter.primary.web.dto.response.ApplicationCredentialValidationWebResponse;
import co.edu.uco.seguridad.pdp.applications.infrastructure.adapter.primary.web.interactor.ValidateApplicationCredentialInteractor;
import co.edu.uco.seguridad.pdp.applications.infrastructure.adapter.primary.web.mapper.ApplicationCredentialValidationResponseMapper;
import co.edu.uco.seguridad.pdp.applications.infrastructure.adapter.primary.web.mapper.ValidateApplicationCredentialRequestMapper;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import reactor.core.publisher.Mono;

import java.util.Objects;

public final class ValidateApplicationCredentialInteractorImpl implements ValidateApplicationCredentialInteractor {

    private final ValidateApplicationCredentialUseCase useCase;

    public ValidateApplicationCredentialInteractorImpl(ValidateApplicationCredentialUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase,
                RequiredArgumentMessages.VALIDATE_APPLICATION_CREDENTIAL_USE_CASE);
    }

    @Override
    public Mono<ApplicationCredentialValidationWebResponse> execute(ValidateApplicationCredentialRawRequest raw) {
        return useCase.execute(ValidateApplicationCredentialRequestMapper.toRequest(raw))
                .map(ApplicationCredentialValidationResponseMapper::toResponse);
    }
}
