package co.edu.uco.seguridad.pdp.recursos.application.port.primary.interactor;

import co.edu.uco.seguridad.pdp.recursos.infrastructure.adapter.primary.web.dto.request.raw.SearchProtectedApplicationsRawRequest;
import co.edu.uco.seguridad.pdp.recursos.infrastructure.adapter.primary.web.dto.response.ProtectedApplicationResponse;
import co.edu.uco.seguridad.shared.contract.ReactiveOperation;
import co.edu.uco.seguridad.shared.web.PageResponse;

/**
 * Puerto primario HTTP: recibe la consulta cruda, mapea, ejecuta el caso de uso y proyecta la página HTTP.
 */
public interface SearchProtectedApplicationsInteractor
        extends ReactiveOperation<SearchProtectedApplicationsRawRequest, PageResponse<ProtectedApplicationResponse>> {
}
