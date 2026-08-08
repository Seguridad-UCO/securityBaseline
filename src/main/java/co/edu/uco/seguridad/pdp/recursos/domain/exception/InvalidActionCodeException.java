package co.edu.uco.seguridad.pdp.recursos.domain.exception;

import co.edu.uco.seguridad.crosscutting.messages.ValueObjectMessages;
import co.edu.uco.seguridad.pdp.commons.exception.InvalidValueException;

public final class InvalidActionCodeException extends InvalidValueException {

    public InvalidActionCodeException(String reason) {
        super("INVALID_ACTION_CODE", ValueObjectMessages.invalidActionCode(reason));
    }
}
