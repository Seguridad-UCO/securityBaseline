package co.edu.uco.seguridad.pdp.identity.domain.model;

import co.edu.uco.seguridad.pdp.commons.exception.InvalidIdentifierException;
import co.edu.uco.seguridad.pdp.commons.message.ValueObjectMessages;

import java.util.Objects;
import java.util.UUID;

/** Identificador subrogado de un usuario propio (nunca el {@code subject} del IdP externo). */
public record UserId(UUID value) {

    public UserId {
        Objects.requireNonNull(value, ValueObjectMessages.Identifier.NULL);
    }

    public static UserId of(String raw) {
        try {
            return new UserId(UUID.fromString(raw));
        } catch (IllegalArgumentException | NullPointerException cause) {
            throw new InvalidIdentifierException("USER_ID", raw);
        }
    }
}
