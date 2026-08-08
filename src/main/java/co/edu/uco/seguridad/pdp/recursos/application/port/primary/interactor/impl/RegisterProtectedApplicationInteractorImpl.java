package co.edu.uco.seguridad.pdp.recursos.application.port.primary.interactor.impl;

import co.edu.uco.seguridad.pdp.recursos.application.usecase.RegisterProtectedApplicationUseCase;
import co.edu.uco.seguridad.pdp.recursos.application.port.primary.dto.response.ProtectedApplicationResponse;
import co.edu.uco.seguridad.pdp.recursos.application.port.primary.dto.request.RegisterProtectedApplicationRequest;
import co.edu.uco.seguridad.pdp.recursos.application.port.primary.interactor.RegisterProtectedApplicationInteractor;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * Delega en el caso de uso. Mantiene el controlador delgado y el caso de uso enfocado en orquestación.
 */
public final class RegisterProtectedApplicationInteractorImpl implements RegisterProtectedApplicationInteractor {

    private final RegisterProtectedApplicationUseCase useCase;

    public RegisterProtectedApplicationInteractorImpl(RegisterProtectedApplicationUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, "se requiere caso de uso de registro");
    }

    @Override
    public Mono<ProtectedApplicationResponse> execute(RegisterProtectedApplicationRequest dto) {
        return useCase.execute(dto);
    }
}
