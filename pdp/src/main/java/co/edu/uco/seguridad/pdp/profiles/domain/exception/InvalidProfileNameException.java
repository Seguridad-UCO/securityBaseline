package co.edu.uco.seguridad.pdp.profiles.domain.exception;

import co.edu.uco.seguridad.pdp.commons.exception.InvalidValueException;

public final class InvalidProfileNameException extends InvalidValueException {

    public InvalidProfileNameException(String reason) {
        super("INVALID_PROFILE_NAME", reason);
    }
}
