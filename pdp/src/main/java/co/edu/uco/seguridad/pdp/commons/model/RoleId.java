package co.edu.uco.seguridad.pdp.commons.model;

import co.edu.uco.seguridad.pdp.commons.exception.InvalidIdentifierException;
import co.edu.uco.seguridad.pdp.commons.message.ValueObjectMessages;

import java.util.Objects;
import java.util.UUID;

/** Identificador de un rol del catálogo (HU-004). Vive en commons porque HU-005 (asignaciones) lo consume. */
public record RoleId(UUID value) {

    public RoleId {
        Objects.requireNonNull(value, ValueObjectMessages.Identifier.NULL);
    }

    public static RoleId of(String raw) {
        try {
            return new RoleId(UUID.fromString(raw));
        } catch (IllegalArgumentException | NullPointerException cause) {
            throw new InvalidIdentifierException("ROLE_ID", raw);
        }
    }
}
