package co.edu.uco.seguridad.pdp.identity.domain.rule;

import co.edu.uco.seguridad.pdp.identity.domain.rule.model.UserExistence;
import co.edu.uco.seguridad.shared.contract.OperationWithoutResult;

/**
 * El usuario referenciado tiene que estar registrado.
 */
public interface UserMustExistRule extends OperationWithoutResult<UserExistence> {
}
