package co.edu.uco.seguridad.pdp.recursos.application.port.primary.interactor.impl;

import co.edu.uco.seguridad.pdp.recursos.application.port.primary.interactor.SearchProtectedApplicationsInteractor;
import co.edu.uco.seguridad.pdp.recursos.application.port.primary.mapper.ProtectedResourceCatalogMapper;
import co.edu.uco.seguridad.pdp.recursos.application.usecase.SearchProtectedApplicationsUseCase;
import co.edu.uco.seguridad.pdp.recursos.infrastructure.adapter.primary.web.dto.request.raw.SearchProtectedApplicationsRawRequest;
import co.edu.uco.seguridad.pdp.recursos.infrastructure.adapter.primary.web.dto.response.ProtectedApplicationResponse;
import co.edu.uco.seguridad.pdp.recursos.infrastructure.adapter.primary.web.mapper.ProtectedApplicationResponseMapper;
import co.edu.uco.seguridad.pdp.recursos.infrastructure.adapter.primary.web.mapper.SearchProtectedApplicationsRequestMapper;
import co.edu.uco.seguridad.shared.web.PageResponse;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * Mapea la consulta HTTP → DTO tipado, ejecuta el caso de uso y proyecta dominio → página HTTP.
 */
public final class SearchProtectedApplicationsInteractorImpl implements SearchProtectedApplicationsInteractor {

    private final SearchProtectedApplicationsUseCase useCase;

    public SearchProtectedApplicationsInteractorImpl(SearchProtectedApplicationsUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, "se requiere caso de uso de búsqueda");
    }

    @Override
    public Mono<PageResponse<ProtectedApplicationResponse>> execute(SearchProtectedApplicationsRawRequest raw) {
        return Mono.fromSupplier(() -> SearchProtectedApplicationsRequestMapper.toRequest(
                        SearchProtectedApplicationsRequestMapper.toValidatedRequest(raw)))
                .flatMap(useCase::execute)
                .map(page -> page.map(ProtectedResourceCatalogMapper::toResponse))
                .map(ProtectedApplicationResponseMapper::toPageResponse);
    }
}
