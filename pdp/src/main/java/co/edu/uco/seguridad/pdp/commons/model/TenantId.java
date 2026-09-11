package co.edu.uco.seguridad.pdp.commons.model;

import co.edu.uco.seguridad.pdp.commons.exception.InvalidTenantIdException;
import co.edu.uco.seguridad.pdp.commons.message.ValueObjectMessages;

import java.util.regex.Pattern;

/**
 * Identificador del núcleo compartido de la organización propietaria de una aplicación protegida.
 *
 * <p>Java puro: sin marco, sin anotaciones, sin setters. El constructor compacto es la única
 * forma de entrada, por lo que una instancia existente ya ha satisfecho todos los invariantes.</p>
 */
public record TenantId(String value) {

    private static final Pattern FORMAT = Pattern.compile("[a-zA-Z0-9][a-zA-Z0-9_-]{1,63}");

    public TenantId {
        if (value == null) {
            throw new InvalidTenantIdException(ValueObjectMessages.VALUE_REQUIRED);
        }
        value = value.trim();
        if (value.isEmpty()) {
            throw new InvalidTenantIdException(ValueObjectMessages.VALUE_REQUIRED);
        }
        if (!FORMAT.matcher(value).matches()) {
            throw new InvalidTenantIdException(ValueObjectMessages.TenantId.FORMAT);
        }
    }
}
