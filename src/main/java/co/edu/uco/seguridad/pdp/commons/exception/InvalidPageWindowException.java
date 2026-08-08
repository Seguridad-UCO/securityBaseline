package co.edu.uco.seguridad.pdp.commons.exception;

import co.edu.uco.seguridad.crosscutting.messages.ValueObjectMessages;

public final class InvalidPageWindowException extends InvalidValueException {

    public InvalidPageWindowException(String reason) {
        super("INVALID_PAGE_WINDOW", ValueObjectMessages.invalidPageWindow(reason));
    }
}
