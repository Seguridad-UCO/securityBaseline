package co.edu.uco.seguridad.pdp.resources.domain.model;

import co.edu.uco.seguridad.pdp.commons.message.ValueObjectMessages;
import co.edu.uco.seguridad.pdp.resources.domain.exception.UnsupportedHttpMethodException;

/**
 * Método HTTP protegido de un endpoint. Cerrado a los verbos que un recurso realmente expone.
 */
public enum HttpVerb {

    GET, POST, PUT, PATCH, DELETE, HEAD, OPTIONS;

    public static HttpVerb parse(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new UnsupportedHttpMethodException(ValueObjectMessages.VALUE_REQUIRED);
        }
        try {
            return HttpVerb.valueOf(raw.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException notAVerb) {
            throw new UnsupportedHttpMethodException(ValueObjectMessages.HttpMethod.UNSUPPORTED);
        }
    }
}
