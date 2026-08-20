package co.edu.uco.seguridad.pdp.identity.infrastructure.adapter.primary.web.interactor;

import co.edu.uco.seguridad.pdp.identity.infrastructure.adapter.primary.web.dto.request.raw.AssignTenantRawRequest;
import co.edu.uco.seguridad.pdp.identity.infrastructure.adapter.primary.web.dto.response.UserWebResponse;
import co.edu.uco.seguridad.shared.contract.ReactiveOperation;

/**
 * Adaptador primario HTTP: recibe el payload crudo, mapea, ejecuta el caso de uso y proyecta la
 * respuesta HTTP.
 */
public interface AssignTenantInteractor extends ReactiveOperation<AssignTenantRawRequest, UserWebResponse> {
}
