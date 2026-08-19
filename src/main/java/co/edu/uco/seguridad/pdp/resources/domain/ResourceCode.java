package co.edu.uco.seguridad.pdp.resources.domain;

import co.edu.uco.seguridad.pdp.commons.message.ValueObjectMessages;
import co.edu.uco.seguridad.pdp.resources.domain.exception.InvalidResourceCodeException;

import java.util.regex.Pattern;

/**
 * Identificador estable de la cosa siendo protegida, por ejemplo {@code estudiantes}. Kebab-case
 * forzado aquí, no en el límite HTTP: el PDP compara políticas contra este valor.
 */
public record ResourceCode(String value) {

    private static final Pattern FORMAT = Pattern.compile("[a-z][a-z0-9-]{1,63}");

    public ResourceCode {
        if (value == null) {
            throw new InvalidResourceCodeException(ValueObjectMessages.VALUE_REQUIRED);
        }
        value = value.trim();
        if (value.isEmpty()) {
            throw new InvalidResourceCodeException(ValueObjectMessages.VALUE_REQUIRED);
        }
        if (!FORMAT.matcher(value).matches()) {
            throw new InvalidResourceCodeException(ValueObjectMessages.ResourceCode.FORMAT);
        }
    }

    public boolean contains(String fragment) {
        return fragment != null && value.contains(fragment.toLowerCase());
    }
}
