package co.edu.uco.seguridad.pdp.commons.exception;

import co.edu.uco.seguridad.pdp.commons.message.ValueObjectMessages;

public final class InvalidTenantIdException extends InvalidValueException {

    public InvalidTenantIdException(String reason) {
        super("INVALID_TENANT_ID", ValueObjectMessages.invalidTenantId(reason));
    }
}
