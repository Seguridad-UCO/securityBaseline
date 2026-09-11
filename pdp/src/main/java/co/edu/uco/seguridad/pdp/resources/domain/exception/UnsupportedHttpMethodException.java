package co.edu.uco.seguridad.pdp.resources.domain.exception;

import co.edu.uco.seguridad.pdp.commons.exception.InvalidValueException;
import co.edu.uco.seguridad.pdp.commons.message.ValueObjectMessages;

public final class UnsupportedHttpMethodException extends InvalidValueException {

    public UnsupportedHttpMethodException(String reason) {
        super("UNSUPPORTED_HTTP_METHOD", ValueObjectMessages.unsupportedHttpMethod(reason));
    }
}
