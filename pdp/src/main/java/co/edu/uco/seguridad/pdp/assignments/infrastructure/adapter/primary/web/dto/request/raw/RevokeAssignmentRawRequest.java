package co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.primary.web.dto.request.raw;

/** El assignmentId lo pone el controller desde la ruta. Sin cuerpo. */
public record RevokeAssignmentRawRequest(String assignmentId) {
}
