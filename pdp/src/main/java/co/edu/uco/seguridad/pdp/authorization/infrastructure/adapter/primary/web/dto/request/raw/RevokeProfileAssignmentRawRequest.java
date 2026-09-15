package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.request.raw;

/** El profileAssignmentId lo pone el controller desde la ruta. Sin cuerpo. Movido desde {@code assignments} (HU-019). */
public record RevokeProfileAssignmentRawRequest(String profileAssignmentId) {
}
