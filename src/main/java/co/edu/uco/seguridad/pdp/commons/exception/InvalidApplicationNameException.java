package co.edu.uco.seguridad.pdp.commons.exception;

import co.edu.uco.seguridad.crosscutting.messages.ValueObjectMessages;

public final class InvalidApplicationNameException extends InvalidValueException {

    public InvalidApplicationNameException(String reason) {
        super("INVALID_APPLICATION_NAME", ValueObjectMessages.invalidApplicationName(reason));
    }
}
