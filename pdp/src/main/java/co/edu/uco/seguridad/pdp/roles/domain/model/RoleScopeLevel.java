package co.edu.uco.seguridad.pdp.roles.domain.model;

import co.edu.uco.seguridad.pdp.commons.message.ValueObjectMessages;
import co.edu.uco.seguridad.pdp.roles.domain.exception.InvalidRoleScopeException;

import java.util.Locale;

/** Nivel de alcance de un rol (INV-DAT-01): global, de inquilino o de aplicación, de forma inequívoca. */
public enum RoleScopeLevel {

    GLOBAL, TENANT, APPLICATION;

    public boolean requiresTenant() {
        return this != GLOBAL;
    }

    public boolean requiresApplication() {
        return this == APPLICATION;
    }

    public static RoleScopeLevel parse(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new InvalidRoleScopeException(ValueObjectMessages.RoleScope.UNSUPPORTED_LEVEL);
        }
        try {
            return RoleScopeLevel.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException notALevel) {
            throw new InvalidRoleScopeException(ValueObjectMessages.RoleScope.UNSUPPORTED_LEVEL);
        }
    }
}
