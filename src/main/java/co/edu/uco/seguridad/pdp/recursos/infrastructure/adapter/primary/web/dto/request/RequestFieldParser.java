package co.edu.uco.seguridad.pdp.recursos.infrastructure.adapter.primary.web.dto.request;

import co.edu.uco.seguridad.crosscutting.messages.WebContractMessages;
import co.edu.uco.seguridad.pdp.commons.exception.InvalidValueException;
import co.edu.uco.seguridad.shared.web.exception.MalformedRequestFieldException;
import co.edu.uco.seguridad.shared.web.exception.MissingRequestFieldException;

import java.util.Optional;
import java.util.function.Function;

/**
 * El vocabulario que los DTOs validados usan dentro de sus setters.
 *
 * <p>{@link #parse} es el importante: delega la decisión de formato al objeto de valor
 * y solo agrega qué campo llevaba el valor incorrecto. Repetir una expresión regular aquí le daría
 * al límite una segunda definición divergente de lo que es un código válido.</p>
 */
final class RequestFieldParser {

    private RequestFieldParser() {
    }

    static String requirePresent(String field, String value) {
        if (value == null || value.isBlank()) {
            throw new MissingRequestFieldException(field);
        }
        return value.trim();
    }

    static <T> T parse(String field, String value, Function<String, T> factory) {
        try {
            return factory.apply(requirePresent(field, value));
        } catch (InvalidValueException cause) {
            throw new MalformedRequestFieldException(field, cause.getMessage());
        }
    }

    static <T> Optional<T> parseOptional(String field, String value, Function<String, T> factory) {
        return optional(value).map(present -> parse(field, present, factory));
    }

    static int parseInt(String field, String value) {
        try {
            return Integer.parseInt(requirePresent(field, value));
        } catch (NumberFormatException cause) {
            throw new MalformedRequestFieldException(field, WebContractMessages.mustBeInteger());
        }
    }

    static Optional<String> optional(String value) {
        return Optional.ofNullable(value).map(String::trim).filter(present -> !present.isEmpty());
    }
}
