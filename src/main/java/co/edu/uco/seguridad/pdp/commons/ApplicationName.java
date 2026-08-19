package co.edu.uco.seguridad.pdp.commons;

import co.edu.uco.seguridad.pdp.commons.exception.InvalidApplicationNameException;
import co.edu.uco.seguridad.pdp.commons.message.ValueObjectMessages;

/**
 * Nombre legible de una aplicación protegida, único por inquilino. Comparación sin distinguir
 * mayúsculas: la unicidad es una regla funcional, no a nivel de bytes.
 */
public record ApplicationName(String value) {

    private static final int MIN_LENGTH = 3;
    private static final int MAX_LENGTH = 100;

    public ApplicationName {
        if (value == null) {
            throw new InvalidApplicationNameException(ValueObjectMessages.VALUE_REQUIRED);
        }
        value = value.trim();
        if (value.isEmpty()) {
            throw new InvalidApplicationNameException(ValueObjectMessages.VALUE_REQUIRED);
        }
        if (value.length() < MIN_LENGTH || value.length() > MAX_LENGTH) {
            throw new InvalidApplicationNameException(ValueObjectMessages.ApplicationName.LENGTH);
        }
    }

    public boolean sameAs(ApplicationName other) {
        return other != null && value.equalsIgnoreCase(other.value);
    }

    public boolean contains(String fragment) {
        return fragment != null && value.toLowerCase().contains(fragment.toLowerCase());
    }
}
