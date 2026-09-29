package co.edu.uco.seguridad.pdp.roles.domain.exception;

import co.edu.uco.seguridad.pdp.commons.exception.BusinessRuleViolationException;
import co.edu.uco.seguridad.pdp.commons.model.RoleId;

/**
 * El rol técnico ADMIN mantiene la administración de su aplicación y es inmutable.
 */
public final class ProtectedRoleException extends BusinessRuleViolationException {

    public ProtectedRoleException(RoleId roleId) {
        super("PROTECTED_ROLE", "No se puede modificar el rol protegido ADMIN " + roleId.value());
    }
}
