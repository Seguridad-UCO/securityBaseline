package co.edu.uco.seguridad.pdp.identity.domain.exception;

import co.edu.uco.seguridad.pdp.commons.exception.InvalidValueException;

public final class InvalidEmailException extends InvalidValueException {

    public InvalidEmailException(String reason) {
        super("INVALID_EMAIL", "El correo es inválido: " + reason);
    }
}
