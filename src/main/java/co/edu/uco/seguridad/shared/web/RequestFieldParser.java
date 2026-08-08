package co.edu.uco.seguridad.shared.web;

import co.edu.uco.seguridad.crosscutting.messages.WebContractMessages;
import co.edu.uco.seguridad.pdp.commons.exception.InvalidValueException;
import co.edu.uco.seguridad.shared.web.exception.MalformedRequestFieldException;
import co.edu.uco.seguridad.shared.web.exception.MissingRequestFieldException;

import java.util.Optional;
import java.util.function.Function;

/**
 * Utilidad compartida del adaptador web: convierte campos String del transporte en tipos tipados,
 * nombrando el campo cuando el value object o el parseo fallan.
 */
public final class RequestFieldParser {

    private RequestFieldParser() {
    }

    public static String requirePresent(String field, String value) {
        if (value == null || value.isBlank()) {
            throw new MissingRequestFieldException(field);
        }
        return value.trim();
    }

    public static <T> T parse(String field, String value, Function<String, T> factory) {
        try {
            return factory.apply(requirePresent(field, value));
        } catch (InvalidValueException cause) {
            throw new MalformedRequestFieldException(field, cause.getMessage());
        }
    }

    public static <T> Optional<T> parseOptional(String field, String value, Function<String, T> factory) {
        return optional(value).map(present -> parse(field, present, factory));
    }

    public static int parseInt(String field, String value) {
        try {
            return Integer.parseInt(requirePresent(field, value));
        } catch (NumberFormatException cause) {
            throw new MalformedRequestFieldException(field, WebContractMessages.mustBeInteger());
        }
    }

    public static Optional<String> optional(String value) {
        return Optional.ofNullable(value).map(String::trim).filter(present -> !present.isEmpty());
    }
}
