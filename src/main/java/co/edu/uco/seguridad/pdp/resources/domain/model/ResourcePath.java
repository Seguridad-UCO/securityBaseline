package co.edu.uco.seguridad.pdp.resources.domain.model;

import co.edu.uco.seguridad.pdp.commons.message.ValueObjectMessages;
import co.edu.uco.seguridad.pdp.resources.domain.exception.InvalidResourcePathException;

/**
 * Ruta de un endpoint, relativa a la {@code baseUrl} de su aplicación. El endpoint completo que una
 * política evalúa es {@code application.baseUrl() + path}.
 */
public record ResourcePath(String value) {

    public ResourcePath {
        if (value == null) {
            throw new InvalidResourcePathException(ValueObjectMessages.VALUE_REQUIRED);
        }
        if (!isValid(value)) {
            throw new InvalidResourcePathException(ValueObjectMessages.ResourcePath.FORMAT);
        }
    }

    private static boolean isValid(String path) {
        if (path.isEmpty() || path.charAt(0) != '/') {
            return false;
        }
        for (int index = 1; index < path.length(); index++) {
            char character = path.charAt(index);
            if (character == '/') {
                if (path.charAt(index - 1) == '/') {
                    return false;
                }
            } else if (!isPathCharacter(character)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isPathCharacter(char character) {
        return character >= 'A' && character <= 'Z' || character >= 'a' && character <= 'z'
                || character >= '0' && character <= '9' || "._~{}-".indexOf(character) >= 0;
    }
}
