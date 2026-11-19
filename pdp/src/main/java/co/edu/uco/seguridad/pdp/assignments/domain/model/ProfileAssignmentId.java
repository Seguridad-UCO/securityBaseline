package co.edu.uco.seguridad.pdp.assignments.domain.model;

import co.edu.uco.seguridad.pdp.commons.exception.InvalidIdentifierException;
import co.edu.uco.seguridad.pdp.commons.message.ValueObjectMessages;

import java.util.Objects;
import java.util.UUID;

/**
 * Identificador de la asignación de un perfil a un usuario (HU-011).
 */
public record ProfileAssignmentId(UUID value) {

    public ProfileAssignmentId {
        Objects.requireNonNull(value, ValueObjectMessages.Identifier.NULL);
    }

    public static ProfileAssignmentId of(String raw) {
        try {
            return new ProfileAssignmentId(UUID.fromString(raw));
        } catch (IllegalArgumentException | NullPointerException cause) {
            throw new InvalidIdentifierException("PROFILE_ASSIGNMENT_ID", raw);
        }
    }
}
