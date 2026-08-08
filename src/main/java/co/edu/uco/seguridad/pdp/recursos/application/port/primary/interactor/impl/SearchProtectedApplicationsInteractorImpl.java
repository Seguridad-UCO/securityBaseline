package co.edu.uco.seguridad.pdp.recursos.application.port.primary.interactor.impl;

import co.edu.uco.seguridad.pdp.commons.ResultPage;
import co.edu.uco.seguridad.pdp.recursos.application.usecase.SearchProtectedApplicationsUseCase;
import co.edu.uco.seguridad.pdp.recursos.application.port.primary.dto.response.ProtectedApplicationResponse;
import co.edu.uco.seguridad.pdp.recursos.application.port.primary.dto.request.SearchProtectedApplicationsRequest;
import co.edu.uco.seguridad.pdp.recursos.application.port.primary.interactor.SearchProtectedApplicationsInteractor;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * Delega en el caso de uso. Mantiene el controlador delgado y el caso de uso enfocado en orquestación.
 */
public final class SearchProtectedApplicationsInteractorImpl implements SearchProtectedApplicationsInteractor {

    private final SearchProtectedApplicationsUseCase useCase;

    public SearchProtectedApplicationsInteractorImpl(SearchProtectedApplicationsUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, "se requiere caso de uso de búsqueda");
    }

    @Override
    public Mono<ResultPage<ProtectedApplicationResponse>> execute(SearchProtectedApplicationsRequest dto) {
        return useCase.execute(dto);
    }
}
