package co.edu.uco.seguridad.pdp.commons.exception;

import co.edu.uco.seguridad.crosscutting.messages.ValueObjectMessages;

public final class InvalidIdentifierException extends InvalidValueException {

    public InvalidIdentifierException(String identifierName, String rejectedValue) {
        super("INVALID_" + identifierName,
                ValueObjectMessages.invalidIdentifier(identifierName, rejectedValue));
    }
}
