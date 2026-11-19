package co.edu.uco.seguridad.pdp.roles.domain.exception;

import co.edu.uco.seguridad.pdp.commons.exception.InvalidValueException;

public final class InvalidRoleNameException extends InvalidValueException {

    public InvalidRoleNameException(String reason) {
        super("INVALID_ROLE_NAME", reason);
    }
}
