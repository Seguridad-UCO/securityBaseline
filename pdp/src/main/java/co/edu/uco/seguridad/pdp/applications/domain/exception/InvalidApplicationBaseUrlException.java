package co.edu.uco.seguridad.pdp.applications.domain.exception;

import co.edu.uco.seguridad.pdp.commons.exception.InvalidValueException;
import co.edu.uco.seguridad.pdp.commons.message.ValueObjectMessages;

public final class InvalidApplicationBaseUrlException extends InvalidValueException {

    public InvalidApplicationBaseUrlException(String reason) {
        super("INVALID_APPLICATION_BASE_URL", ValueObjectMessages.invalidApplicationBaseUrl(reason));
    }
}
