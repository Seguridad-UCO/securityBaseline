package co.edu.uco.seguridad.pdp.commons.model;

import co.edu.uco.seguridad.pdp.commons.exception.InvalidIdentifierException;
import co.edu.uco.seguridad.pdp.commons.message.ValueObjectMessages;

import java.util.Objects;
import java.util.UUID;

public record ApplicationId(UUID value) {

    public ApplicationId {
        Objects.requireNonNull(value, ValueObjectMessages.Identifier.NULL);
    }

    public static ApplicationId of(String raw) {
        try {
            return new ApplicationId(UUID.fromString(raw));
        } catch (IllegalArgumentException | NullPointerException cause) {
            throw new InvalidIdentifierException("APPLICATION_ID", raw);
        }
    }
}
