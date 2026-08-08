package co.edu.uco.seguridad.pdp.commons;

import co.edu.uco.seguridad.crosscutting.messages.ValueObjectMessages;
import co.edu.uco.seguridad.pdp.commons.exception.InvalidApplicationNameException;

/**
 * Nombre legible de una aplicación protegida, único por inquilino.
 *
 * <p>Vive en el núcleo compartido porque tanto {@code aplicaciones} (que posee el registro)
 * como {@code recursos} (que mantiene el modelo de lectura del catálogo) hablan del mismo concepto.
 * La comparación no distingue entre mayúsculas y minúsculas porque la unicidad es una regla funcional, no a nivel de bytes.</p>
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
