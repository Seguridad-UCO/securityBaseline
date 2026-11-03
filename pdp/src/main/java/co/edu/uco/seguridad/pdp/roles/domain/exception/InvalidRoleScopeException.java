package co.edu.uco.seguridad.pdp.roles.domain.exception;

import co.edu.uco.seguridad.pdp.commons.exception.InvalidValueException;

public final class InvalidRoleScopeException extends InvalidValueException {

    public InvalidRoleScopeException(String reason) {
        super("INVALID_ROLE_SCOPE", reason);
    }
}
