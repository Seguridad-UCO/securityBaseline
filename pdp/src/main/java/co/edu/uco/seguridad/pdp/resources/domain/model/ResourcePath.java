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

    /**
     * Determines whether this registered resource path covers a concrete request path.
     * A path variable is a complete segment in the form {@code {name}}; it matches exactly
     * one non-empty path segment. It deliberately does not match a slash, query string or
     * a partial segment, so a template cannot widen the resource it protects.
     */
    public boolean matches(ResourcePath requestPath) {
        String[] templateSegments = value.substring(1).split("/", -1);
        String[] requestSegments = requestPath.value.substring(1).split("/", -1);
        if (templateSegments.length != requestSegments.length) return false;
        for (int index = 0; index < templateSegments.length; index++) {
            if (!isTemplateSegment(templateSegments[index]) && !templateSegments[index].equals(requestSegments[index])) {
                return false;
            }
        }
        return true;
    }

    private static boolean isTemplateSegment(String segment) {
        return segment.matches("\\{[A-Za-z][A-Za-z0-9_-]*}");
    }

    private static boolean isPathCharacter(char character) {
        return character >= 'A' && character <= 'Z' || character >= 'a' && character <= 'z'
                || character >= '0' && character <= '9' || "._~{}-".indexOf(character) >= 0;
    }
}
