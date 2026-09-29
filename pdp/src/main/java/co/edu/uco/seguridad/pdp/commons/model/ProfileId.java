package co.edu.uco.seguridad.pdp.commons.model;

import co.edu.uco.seguridad.pdp.commons.exception.InvalidIdentifierException;
import co.edu.uco.seguridad.pdp.commons.message.ValueObjectMessages;

import java.util.Objects;
import java.util.UUID;

/**
 * Identificador de un perfil del catálogo (HU-011). Vive en commons porque assignments lo consume.
 */
public record ProfileId(UUID value) {

    public ProfileId {
        Objects.requireNonNull(value, ValueObjectMessages.Identifier.NULL);
    }

    public static ProfileId of(String raw) {
        try {
            return new ProfileId(UUID.fromString(raw));
        } catch (IllegalArgumentException | NullPointerException cause) {
            throw new InvalidIdentifierException("PROFILE_ID", raw);
        }
    }
}
