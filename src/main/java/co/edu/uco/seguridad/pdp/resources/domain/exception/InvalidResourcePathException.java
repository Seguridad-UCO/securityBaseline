package co.edu.uco.seguridad.pdp.resources.domain.exception;

import co.edu.uco.seguridad.pdp.commons.exception.InvalidValueException;
import co.edu.uco.seguridad.pdp.commons.message.ValueObjectMessages;

public final class InvalidResourcePathException extends InvalidValueException {

    public InvalidResourcePathException(String reason) {
        super("INVALID_RESOURCE_PATH", ValueObjectMessages.invalidResourcePath(reason));
    }
}
