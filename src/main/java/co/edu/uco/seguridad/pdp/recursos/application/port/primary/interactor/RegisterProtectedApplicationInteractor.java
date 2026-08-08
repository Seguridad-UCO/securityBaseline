package co.edu.uco.seguridad.pdp.recursos.application.port.primary.interactor;

import co.edu.uco.seguridad.pdp.recursos.infrastructure.adapter.primary.web.dto.request.raw.RegisterProtectedApplicationRawRequest;
import co.edu.uco.seguridad.pdp.recursos.infrastructure.adapter.primary.web.dto.response.ProtectedApplicationResponse;
import co.edu.uco.seguridad.shared.contract.ReactiveOperation;

/**
 * Puerto primario HTTP: recibe el payload crudo, mapea, ejecuta el caso de uso y proyecta la respuesta HTTP.
 */
public interface RegisterProtectedApplicationInteractor
        extends ReactiveOperation<RegisterProtectedApplicationRawRequest, ProtectedApplicationResponse> {
}
