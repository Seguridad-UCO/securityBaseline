package co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.primary.web.dto.request.raw;

/** El profileAssignmentId lo pone el controller desde la ruta. Sin cuerpo. */
public record RevokeProfileAssignmentRawRequest(String profileAssignmentId) {
}
