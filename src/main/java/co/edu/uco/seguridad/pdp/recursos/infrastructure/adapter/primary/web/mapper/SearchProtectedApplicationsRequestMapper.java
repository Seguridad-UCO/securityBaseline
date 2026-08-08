package co.edu.uco.seguridad.pdp.recursos.infrastructure.adapter.primary.web.mapper;

import co.edu.uco.seguridad.pdp.recursos.domain.ProtectedApplicationCriteria;
import co.edu.uco.seguridad.pdp.recursos.infrastructure.adapter.primary.web.dto.request.SearchProtectedApplicationsRequest;
import co.edu.uco.seguridad.pdp.recursos.infrastructure.adapter.primary.web.dto.request.raw.SearchProtectedApplicationsRawRequest;

/**
 * Los mismos dos pasos que el mapper de registro: validar a través de los setters, luego traducir.
 *
 * <p>El objeto de criterios se ensambla aquí en lugar de en el controlador, así que el punto de
 * entrada nunca construye una consulta — solo entrega parámetros.</p>
 */
public final class SearchProtectedApplicationsRequestMapper {

    private SearchProtectedApplicationsRequestMapper() {
    }

    public static SearchProtectedApplicationsRequest toValidatedRequest(SearchProtectedApplicationsRawRequest raw) {
        SearchProtectedApplicationsRequest request = new SearchProtectedApplicationsRequest();
        request.setTenantId(raw.tenantId());
        request.setNameContains(raw.nameContains());
        request.setResourceContains(raw.resourceContains());
        request.setResultWindow(raw.page(), raw.size(), raw.offset(), raw.limit());
        return request;
    }

    public static co.edu.uco.seguridad.pdp.recursos.application.port.primary.dto.request.SearchProtectedApplicationsRequest toRequest(
            SearchProtectedApplicationsRequest request) {
        return new co.edu.uco.seguridad.pdp.recursos.application.port.primary.dto.request.SearchProtectedApplicationsRequest(
                new ProtectedApplicationCriteria(
                        request.tenantId(), request.nameContains(), request.resourceContains()),
                request.window());
    }
}
