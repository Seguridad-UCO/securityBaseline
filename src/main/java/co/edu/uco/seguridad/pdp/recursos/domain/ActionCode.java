package co.edu.uco.seguridad.pdp.recursos.domain;

import co.edu.uco.seguridad.crosscutting.messages.ValueObjectMessages;
import co.edu.uco.seguridad.pdp.recursos.domain.exception.InvalidActionCodeException;

import java.util.regex.Pattern;

/**
 * Operación permitida en un recurso, por ejemplo {@code consultar}. Misma disciplina kebab-case que
 * {@link ResourceCode}, y por la misma razón de coincidencia de política.
 */
public record ActionCode(String value) {

    private static final Pattern FORMAT = Pattern.compile("[a-z][a-z0-9-]{1,63}");

    public ActionCode {
        if (value == null) {
            throw new InvalidActionCodeException(ValueObjectMessages.VALUE_REQUIRED);
        }
        value = value.trim();
        if (value.isEmpty()) {
            throw new InvalidActionCodeException(ValueObjectMessages.VALUE_REQUIRED);
        }
        if (!FORMAT.matcher(value).matches()) {
            throw new InvalidActionCodeException(ValueObjectMessages.ActionCode.FORMAT);
        }
    }
}
