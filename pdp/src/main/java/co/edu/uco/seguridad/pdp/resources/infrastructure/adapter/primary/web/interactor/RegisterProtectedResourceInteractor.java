package co.edu.uco.seguridad.pdp.resources.infrastructure.adapter.primary.web.interactor;

import co.edu.uco.seguridad.pdp.resources.infrastructure.adapter.primary.web.dto.request.raw.RegisterProtectedResourceRawRequest;
import co.edu.uco.seguridad.pdp.resources.infrastructure.adapter.primary.web.dto.response.ProtectedResourceWebResponse;
import co.edu.uco.seguridad.shared.contract.ReactiveOperation;

/**
 * Adaptador primario HTTP: recibe el payload crudo, mapea, ejecuta el caso de uso y proyecta la
 * respuesta HTTP.
 */
public interface RegisterProtectedResourceInteractor
        extends ReactiveOperation<RegisterProtectedResourceRawRequest, ProtectedResourceWebResponse> {
}
