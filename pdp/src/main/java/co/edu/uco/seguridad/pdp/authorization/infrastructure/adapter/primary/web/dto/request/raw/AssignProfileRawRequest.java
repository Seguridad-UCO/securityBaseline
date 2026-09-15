package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.request.raw;

/** El profileId lo pone el controller desde la ruta; userId y applicationId vienen en el cuerpo. Movido desde {@code assignments} (HU-019). */
public record AssignProfileRawRequest(String profileId, String userId, String applicationId) {
}
