package co.edu.uco.seguridad.pdp.applications.domain;

import co.edu.uco.seguridad.pdp.applications.domain.exception.InvalidApplicationBaseUrlException;
import co.edu.uco.seguridad.pdp.commons.message.ValueObjectMessages;

import java.net.URI;

/**
 * URL base de la aplicación externa que este catálogo protege. Los recursos registrados bajo esta
 * aplicación son rutas relativas a esta base — el endpoint completo es {@code baseUrl + path}.
 */
public record ApplicationBaseUrl(String value) {

    public ApplicationBaseUrl {
        if (value == null) {
            throw new InvalidApplicationBaseUrlException(ValueObjectMessages.VALUE_REQUIRED);
        }
        value = value.trim();
        if (value.isEmpty() || !isAbsolute(value)) {
            throw new InvalidApplicationBaseUrlException(ValueObjectMessages.ApplicationBaseUrl.FORMAT);
        }
    }

    private static boolean isAbsolute(String value) {
        try {
            URI uri = URI.create(value);
            return uri.getScheme() != null && uri.getHost() != null;
        } catch (IllegalArgumentException malformed) {
            return false;
        }
    }
}
