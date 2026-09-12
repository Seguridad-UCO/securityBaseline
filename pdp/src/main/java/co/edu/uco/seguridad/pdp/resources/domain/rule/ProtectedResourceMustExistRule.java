package co.edu.uco.seguridad.pdp.resources.domain.rule;

import co.edu.uco.seguridad.pdp.resources.domain.rule.model.ProtectedResourceExistence;
import co.edu.uco.seguridad.shared.contract.OperationWithoutResult;

/** El recurso protegido referenciado tiene que existir bajo esa aplicacion. */
public interface ProtectedResourceMustExistRule extends OperationWithoutResult<ProtectedResourceExistence> {
}
