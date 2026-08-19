package co.edu.uco.seguridad.pdp.resources.infrastructure.adapter.primary.web.interactor;

import co.edu.uco.seguridad.pdp.resources.infrastructure.adapter.primary.web.dto.request.raw.SearchProtectedApplicationsRawRequest;
import co.edu.uco.seguridad.pdp.resources.infrastructure.adapter.primary.web.dto.response.ProtectedApplicationResponse;
import co.edu.uco.seguridad.shared.contract.ReactiveOperation;
import co.edu.uco.seguridad.shared.web.PageResponse;

/**
 * Adaptador primario HTTP: recibe la consulta cruda, mapea, ejecuta el caso de uso y proyecta la
 * página HTTP. Vive en {@code infrastructure} por el mismo motivo que
 * {@link RegisterProtectedApplicationInteractor}.
 */
public interface SearchProtectedApplicationsInteractor
        extends ReactiveOperation<SearchProtectedApplicationsRawRequest, PageResponse<ProtectedApplicationResponse>> {
}
