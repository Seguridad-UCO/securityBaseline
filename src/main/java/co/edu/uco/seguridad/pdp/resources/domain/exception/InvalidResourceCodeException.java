package co.edu.uco.seguridad.pdp.resources.domain.exception;

import co.edu.uco.seguridad.pdp.commons.exception.InvalidValueException;
import co.edu.uco.seguridad.pdp.commons.message.ValueObjectMessages;

public final class InvalidResourceCodeException extends InvalidValueException {

    public InvalidResourceCodeException(String reason) {
        super("INVALID_RESOURCE_CODE", ValueObjectMessages.invalidResourceCode(reason));
    }
}
