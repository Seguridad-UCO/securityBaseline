package co.edu.uco.seguridad.pdp.aplicaciones.application.port.primary.interactor.impl;

import co.edu.uco.seguridad.pdp.aplicaciones.application.port.primary.interactor.RemoveApplicationInteractor;
import co.edu.uco.seguridad.pdp.aplicaciones.application.usecase.RemoveApplicationUseCase;
import co.edu.uco.seguridad.pdp.commons.ApplicationId;
import reactor.core.publisher.Mono;

import java.util.Objects;

public final class RemoveApplicationInteractorImpl implements RemoveApplicationInteractor {

    private final RemoveApplicationUseCase useCase;

    public RemoveApplicationInteractorImpl(RemoveApplicationUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, "se requiere caso de uso de eliminación");
    }

    @Override
    public Mono<Void> execute(ApplicationId applicationId) {
        return useCase.execute(applicationId);
    }
}
