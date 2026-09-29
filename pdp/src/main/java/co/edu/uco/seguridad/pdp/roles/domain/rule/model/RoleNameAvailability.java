package co.edu.uco.seguridad.pdp.roles.domain.rule.model;

import co.edu.uco.seguridad.pdp.roles.domain.model.RoleName;
import co.edu.uco.seguridad.pdp.roles.domain.model.RoleScope;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;

import java.util.Objects;

/**
 * Hecho ya resuelto para RoleNameMustBeUniqueInScopeRule: si el nombre ya está tomado en ese alcance.
 */
public record RoleNameAvailability(RoleName name, RoleScope scope, boolean taken) {

    public RoleNameAvailability {
        Objects.requireNonNull(name, RequiredArgumentMessages.ROLE_NAME);
        Objects.requireNonNull(scope, RequiredArgumentMessages.ROLE_SCOPE);
    }
}
