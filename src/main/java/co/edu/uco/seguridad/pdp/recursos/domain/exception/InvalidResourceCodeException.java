package co.edu.uco.seguridad.pdp.recursos.domain.exception;

import co.edu.uco.seguridad.crosscutting.messages.ValueObjectMessages;
import co.edu.uco.seguridad.pdp.commons.exception.InvalidValueException;

public final class InvalidResourceCodeException extends InvalidValueException {

    public InvalidResourceCodeException(String reason) {
        super("INVALID_RESOURCE_CODE", ValueObjectMessages.invalidResourceCode(reason));
    }
}
