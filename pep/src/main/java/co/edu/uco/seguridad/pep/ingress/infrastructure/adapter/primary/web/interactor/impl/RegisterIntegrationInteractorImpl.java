package co.edu.uco.seguridad.pep.ingress.infrastructure.adapter.primary.web.interactor.impl;

import co.edu.uco.seguridad.pep.ingress.application.usecase.RegisterIntegrationUseCase;
import co.edu.uco.seguridad.pep.ingress.infrastructure.adapter.primary.web.dto.request.raw.RegisterIntegrationRawRequest;
import co.edu.uco.seguridad.pep.ingress.infrastructure.adapter.primary.web.dto.response.IntegrationWebResponse;
import co.edu.uco.seguridad.pep.ingress.infrastructure.adapter.primary.web.interactor.RegisterIntegrationInteractor;
import co.edu.uco.seguridad.pep.ingress.infrastructure.adapter.primary.web.mapper.IntegrationResponseMapper;
import co.edu.uco.seguridad.pep.ingress.infrastructure.adapter.primary.web.mapper.RegisterIntegrationRequestMapper;
import reactor.core.publisher.Mono;

import java.util.Objects;

public final class RegisterIntegrationInteractorImpl implements RegisterIntegrationInteractor {
    private final RegisterIntegrationUseCase useCase;

    public RegisterIntegrationInteractorImpl(RegisterIntegrationUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase);
    }

    @Override
    public Mono<IntegrationWebResponse> execute(RegisterIntegrationRawRequest raw) {
        return Mono.just(raw).map(RegisterIntegrationRequestMapper::toRequest)
                .flatMap(useCase::execute).map(IntegrationResponseMapper::toResponse);
    }
}
