package co.edu.uco.seguridad.pdp.applications.infrastructure.adapter.primary.web.interactor;

import co.edu.uco.seguridad.pdp.applications.infrastructure.adapter.primary.web.dto.request.raw.RegisterApplicationRawRequest;
import co.edu.uco.seguridad.pdp.applications.infrastructure.adapter.primary.web.dto.response.ApplicationWebResponse;
import co.edu.uco.seguridad.shared.contract.ReactiveOperation;

/**
 * Adaptador primario HTTP: recibe el payload crudo, mapea, ejecuta el caso de uso y proyecta la
 * respuesta HTTP.
 */
public interface RegisterApplicationInteractor
        extends ReactiveOperation<RegisterApplicationRawRequest, ApplicationWebResponse> {
}
