package co.edu.uco.seguridad.pdp.tenants.domain.model;

import co.edu.uco.seguridad.pdp.commons.message.ValueObjectMessages;
import co.edu.uco.seguridad.pdp.tenants.domain.exception.InvalidTenantNameException;

/** Nombre legible del tenant, distinto de {@link co.edu.uco.seguridad.pdp.commons.TenantId} (el slug). */
public record TenantName(String value) {

    private static final int MIN_LENGTH = 3;
    private static final int MAX_LENGTH = 100;

    public TenantName {
        if (value == null) {
            throw new InvalidTenantNameException(ValueObjectMessages.VALUE_REQUIRED);
        }
        value = value.trim();
        if (value.isEmpty()) {
            throw new InvalidTenantNameException(ValueObjectMessages.VALUE_REQUIRED);
        }
        if (value.length() < MIN_LENGTH || value.length() > MAX_LENGTH) {
            throw new InvalidTenantNameException(ValueObjectMessages.TenantName.LENGTH);
        }
    }
}
