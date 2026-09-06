package co.edu.uco.seguridad.pdp.resources.domain.rule;

import co.edu.uco.seguridad.pdp.resources.domain.rule.model.ProtectedResourceAvailability;
import co.edu.uco.seguridad.shared.contract.OperationWithoutResult;

/** El mismo método sobre la misma ruta no puede registrarse dos veces en una aplicación. */
public interface ProtectedResourceMustBeUniqueRule extends OperationWithoutResult<ProtectedResourceAvailability> {
}
