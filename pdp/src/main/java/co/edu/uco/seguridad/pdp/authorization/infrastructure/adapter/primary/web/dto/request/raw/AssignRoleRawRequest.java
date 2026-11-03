package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.request.raw;

/**
 * El roleId lo pone el controller desde la ruta; userId y applicationId vienen en el cuerpo. Movido desde {@code assignments} (HU-018).
 */
public record AssignRoleRawRequest(String roleId, String userId, String applicationId) {
}
