package co.edu.uco.seguridad.pdp.applications.domain.exception;

import co.edu.uco.seguridad.pdp.commons.exception.InvalidValueException;
import co.edu.uco.seguridad.pdp.commons.message.ValueObjectMessages;

public final class InvalidApplicationCredentialHashException extends InvalidValueException {

    public InvalidApplicationCredentialHashException(String reason) {
        super("INVALID_APPLICATION_CREDENTIAL_HASH", ValueObjectMessages.invalidApplicationCredentialHash(reason));
    }
}
