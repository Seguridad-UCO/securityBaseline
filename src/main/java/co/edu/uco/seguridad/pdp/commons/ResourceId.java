package co.edu.uco.seguridad.pdp.commons;

import co.edu.uco.seguridad.crosscutting.messages.ValueObjectMessages;
import co.edu.uco.seguridad.pdp.commons.exception.InvalidIdentifierException;

import java.util.Objects;
import java.util.UUID;

public record ResourceId(UUID value) {

    public ResourceId {
        Objects.requireNonNull(value, ValueObjectMessages.Identifier.NULL);
    }

    public static ResourceId of(String raw) {
        try {
            return new ResourceId(UUID.fromString(raw));
        } catch (IllegalArgumentException | NullPointerException cause) {
            throw new InvalidIdentifierException("RESOURCE_ID", raw);
        }
    }
}
