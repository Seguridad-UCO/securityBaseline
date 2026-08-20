package co.edu.uco.seguridad.pdp.tenants.domain.exception;

import co.edu.uco.seguridad.pdp.commons.exception.InvalidValueException;
import co.edu.uco.seguridad.pdp.commons.message.ValueObjectMessages;

public final class InvalidTenantNameException extends InvalidValueException {

    public InvalidTenantNameException(String reason) {
        super("INVALID_TENANT_NAME", ValueObjectMessages.invalidTenantName(reason));
    }
}
