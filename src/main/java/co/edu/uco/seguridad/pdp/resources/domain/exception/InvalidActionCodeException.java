package co.edu.uco.seguridad.pdp.resources.domain.exception;

import co.edu.uco.seguridad.pdp.commons.exception.InvalidValueException;
import co.edu.uco.seguridad.pdp.commons.message.ValueObjectMessages;

public final class InvalidActionCodeException extends InvalidValueException {

    public InvalidActionCodeException(String reason) {
        super("INVALID_ACTION_CODE", ValueObjectMessages.invalidActionCode(reason));
    }
}
