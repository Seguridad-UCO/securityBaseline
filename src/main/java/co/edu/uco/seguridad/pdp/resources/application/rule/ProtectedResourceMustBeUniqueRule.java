package co.edu.uco.seguridad.pdp.resources.application.rule;

import co.edu.uco.seguridad.pdp.resources.application.port.primary.dto.request.RegisterProtectedResourceRequest;
import co.edu.uco.seguridad.shared.contract.ReactiveOperationWithoutResult;

/** Regla con repositorio: el mismo método sobre la misma ruta no puede registrarse dos veces por aplicación. */
public interface ProtectedResourceMustBeUniqueRule extends ReactiveOperationWithoutResult<RegisterProtectedResourceRequest> {
}
