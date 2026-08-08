package co.edu.uco.seguridad.pdp.recursos.domain;

import co.edu.uco.seguridad.crosscutting.messages.ValueObjectMessages;
import co.edu.uco.seguridad.pdp.recursos.domain.exception.InvalidResourceCodeException;

import java.util.regex.Pattern;

/**
 * Identificador estable de la cosa siendo protegida, por ejemplo {@code estudiantes}.
 *
 * <p>El kebab-case se aplica aquí en lugar del límite HTTP porque el PDP coincidirá
 * políticas en este valor: un código que difiere solo en caso se convertiría silenciosamente en un segundo,
 * recurso inaccesible.</p>
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
