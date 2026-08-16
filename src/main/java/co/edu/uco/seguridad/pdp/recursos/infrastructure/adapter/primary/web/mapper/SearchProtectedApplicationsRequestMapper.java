package co.edu.uco.seguridad.pdp.recursos.infrastructure.adapter.primary.web.mapper;

import co.edu.uco.seguridad.pdp.commons.TenantId;
import co.edu.uco.seguridad.pdp.recursos.domain.ProtectedApplicationCriteria;
import co.edu.uco.seguridad.pdp.recursos.infrastructure.adapter.primary.web.dto.request.SearchProtectedApplicationsRequest;
import co.edu.uco.seguridad.pdp.recursos.infrastructure.adapter.primary.web.dto.request.raw.SearchProtectedApplicationsRawRequest;

/**
 * Los mismos dos pasos que el mapper de registro: validar por setters, luego traducir. El objeto de
 * criterios se ensambla aquí, no en el controlador. {@code tenantId} llega del interactor (ADR-018).
 */
public final class SearchProtectedApplicationsRequestMapper {

    private SearchProtectedApplicationsRequestMapper() {
    }

    public static SearchProtectedApplicationsRequest toValidatedRequest(SearchProtectedApplicationsRawRequest raw) {
        SearchProtectedApplicationsRequest request = new SearchProtectedApplicationsRequest();
        request.setNameContains(raw.nameContains());
        request.setResourceContains(raw.resourceContains());
        request.setResultWindow(raw.page(), raw.size(), raw.offset(), raw.limit());
        return request;
    }

    public static co.edu.uco.seguridad.pdp.recursos.application.port.primary.dto.request.SearchProtectedApplicationsRequest toRequest(
            SearchProtectedApplicationsRequest request, TenantId tenantId) {
        return new co.edu.uco.seguridad.pdp.recursos.application.port.primary.dto.request.SearchProtectedApplicationsRequest(
                new ProtectedApplicationCriteria(
                        tenantId, request.nameContains(), request.resourceContains()),
                request.window());
    }
}
