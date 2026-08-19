package co.edu.uco.seguridad.pdp.commons.exception;

import co.edu.uco.seguridad.pdp.commons.message.ValueObjectMessages;

public final class InvalidPageWindowException extends InvalidValueException {

    public InvalidPageWindowException(String reason) {
        super("INVALID_PAGE_WINDOW", ValueObjectMessages.invalidPageWindow(reason));
    }
}
