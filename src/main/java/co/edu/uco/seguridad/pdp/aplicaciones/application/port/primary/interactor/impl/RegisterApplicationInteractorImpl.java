package co.edu.uco.seguridad.pdp.aplicaciones.application.port.primary.interactor.impl;

import co.edu.uco.seguridad.crosscutting.messages.RequiredArgumentMessages;
import co.edu.uco.seguridad.pdp.aplicaciones.application.port.primary.dto.request.RegisterApplicationRequest;
import co.edu.uco.seguridad.pdp.aplicaciones.application.port.primary.dto.response.RegisteredApplicationResponse;
import co.edu.uco.seguridad.pdp.aplicaciones.application.port.primary.interactor.RegisterApplicationInteractor;
import co.edu.uco.seguridad.pdp.aplicaciones.application.usecase.RegisterApplicationUseCase;
import reactor.core.publisher.Mono;

import java.util.Objects;

public final class RegisterApplicationInteractorImpl implements RegisterApplicationInteractor {

    private final RegisterApplicationUseCase useCase;

    public RegisterApplicationInteractorImpl(RegisterApplicationUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, RequiredArgumentMessages.REGISTER_USE_CASE);
    }

    @Override
    public Mono<RegisteredApplicationResponse> execute(RegisterApplicationRequest dto) {
        return useCase.execute(dto);
    }
}
