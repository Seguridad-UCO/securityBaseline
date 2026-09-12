package co.edu.uco.seguridad.pdp.roles.domain.exception;

import co.edu.uco.seguridad.pdp.commons.exception.ConflictBusinessRuleException;
import co.edu.uco.seguridad.pdp.roles.domain.message.RolesMessages;
import co.edu.uco.seguridad.pdp.roles.domain.model.RoleName;
import co.edu.uco.seguridad.pdp.roles.domain.model.RoleScope;

/** Ya hay un rol con ese nombre en ese alcance exacto (nivel + inquilino + aplicación). */
public final class DuplicateRoleNameException extends ConflictBusinessRuleException {

    public DuplicateRoleNameException(RoleName name, RoleScope scope) {
        super("ROLE_NAME_TAKEN", RolesMessages.roleNameTaken(name.value(), scope.level().name()));
    }
}
