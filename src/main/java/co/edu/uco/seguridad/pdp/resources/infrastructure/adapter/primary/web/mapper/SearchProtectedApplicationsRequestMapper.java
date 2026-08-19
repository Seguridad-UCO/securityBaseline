package co.edu.uco.seguridad.pdp.resources.infrastructure.adapter.primary.web.mapper;

import co.edu.uco.seguridad.pdp.commons.TenantId;
import co.edu.uco.seguridad.pdp.resources.domain.ProtectedApplicationCriteria;
import co.edu.uco.seguridad.pdp.resources.infrastructure.adapter.primary.web.dto.request.SearchProtectedApplicationsQueryRequest;
import co.edu.uco.seguridad.pdp.resources.infrastructure.adapter.primary.web.dto.request.raw.SearchProtectedApplicationsRawRequest;

/**
 * Los mismos dos pasos que el mapper de registro: validar por setters, luego traducir. El objeto de
 * criterios se ensambla aquí, no en el controlador. {@code tenantId} llega del interactor (ADR-018).
 */
public final class SearchProtectedApplicationsRequestMapper {

    private SearchProtectedApplicationsRequestMapper() {
    }

    public static SearchProtectedApplicationsQueryRequest toValidatedRequest(SearchProtectedApplicationsRawRequest raw) {
        SearchProtectedApplicationsQueryRequest request = new SearchProtectedApplicationsQueryRequest();
        request.setNameContains(raw.nameContains());
        request.setResourceContains(raw.resourceContains());
        request.setResultWindow(raw.page(), raw.size(), raw.offset(), raw.limit());
        return request;
    }

    public static co.edu.uco.seguridad.pdp.resources.application.port.primary.dto.request.SearchProtectedApplicationsRequest toRequest(
            SearchProtectedApplicationsQueryRequest request, TenantId tenantId) {
        return new co.edu.uco.seguridad.pdp.resources.application.port.primary.dto.request.SearchProtectedApplicationsRequest(
                new ProtectedApplicationCriteria(
                        tenantId, request.nameContains(), request.resourceContains()),
                request.window());
    }
}
