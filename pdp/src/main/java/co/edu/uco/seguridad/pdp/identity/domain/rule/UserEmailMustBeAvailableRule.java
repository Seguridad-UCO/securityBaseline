package co.edu.uco.seguridad.pdp.identity.domain.rule;

import co.edu.uco.seguridad.pdp.identity.domain.rule.model.UserEmailAvailability;
import co.edu.uco.seguridad.shared.contract.OperationWithoutResult;

/**
 * El correo con el que se aprovisiona un usuario nuevo no puede pertenecer ya a otro (HU-038).
 */
public interface UserEmailMustBeAvailableRule extends OperationWithoutResult<UserEmailAvailability> {
}
