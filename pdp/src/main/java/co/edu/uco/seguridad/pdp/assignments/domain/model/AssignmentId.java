package co.edu.uco.seguridad.pdp.assignments.domain.model;

import co.edu.uco.seguridad.pdp.commons.exception.InvalidIdentifierException;
import co.edu.uco.seguridad.pdp.commons.message.ValueObjectMessages;

import java.util.Objects;
import java.util.UUID;

/** Identificador de una asignación (usuario, aplicación, rol) del catálogo (HU-005). */
public record AssignmentId(UUID value) {

    public AssignmentId {
        Objects.requireNonNull(value, ValueObjectMessages.Identifier.NULL);
    }

    public static AssignmentId of(String raw) {
        try {
            return new AssignmentId(UUID.fromString(raw));
        } catch (IllegalArgumentException | NullPointerException cause) {
            throw new InvalidIdentifierException("ASSIGNMENT_ID", raw);
        }
    }
}
