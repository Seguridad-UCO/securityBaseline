package co.edu.uco.seguridad.pdp.identity.domain.model;

import co.edu.uco.seguridad.pdp.commons.message.ValueObjectMessages;
import co.edu.uco.seguridad.pdp.identity.domain.exception.InvalidEmailException;

import java.util.regex.Pattern;

/**
 * Correo verificado por el IdP. Comparación siempre en minúsculas: la unicidad no distingue caso.
 */
public record Email(String value) {

    private static final Pattern FORMAT = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    public Email {
        if (value == null) {
            throw new InvalidEmailException(ValueObjectMessages.VALUE_REQUIRED);
        }
        value = value.trim().toLowerCase(java.util.Locale.ROOT);
        if (value.isEmpty() || !FORMAT.matcher(value).matches()) {
            throw new InvalidEmailException("debe tener el formato usuario@dominio");
        }
    }
}
