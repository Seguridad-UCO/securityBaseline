package co.edu.uco.seguridad.pdp.roles.domain.exception;

import co.edu.uco.seguridad.pdp.commons.exception.BusinessRuleViolationException;
import co.edu.uco.seguridad.pdp.commons.model.RoleId;
import co.edu.uco.seguridad.pdp.roles.domain.message.RolesMessages;

/**
 * El rol no existe, o no es de este inquilino: un rol ajeno o global no existe para quien pregunta.
 */
public final class RoleNotFoundException extends BusinessRuleViolationException {

    public RoleNotFoundException(RoleId roleId) {
        super("ROLE_NOT_FOUND", RolesMessages.roleNotFound(roleId.value().toString()));
    }
}
