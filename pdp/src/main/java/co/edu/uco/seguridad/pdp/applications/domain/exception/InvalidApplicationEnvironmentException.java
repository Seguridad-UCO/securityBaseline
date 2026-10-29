package co.edu.uco.seguridad.pdp.applications.domain.exception;

import co.edu.uco.seguridad.pdp.commons.exception.InvalidValueException;

/** El ambiente debe ser uno de dev, lab o prod. */
public final class InvalidApplicationEnvironmentException extends InvalidValueException {
    public InvalidApplicationEnvironmentException() {
        super("INVALID_APPLICATION_ENVIRONMENT", "El ambiente debe ser dev, lab o prod");
    }
}
