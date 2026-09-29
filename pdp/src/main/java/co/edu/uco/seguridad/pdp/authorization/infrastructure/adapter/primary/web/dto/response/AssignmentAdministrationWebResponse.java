package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.response;

/**
 * Respuesta plana, misma forma que {@code AssignmentWebResponse} (HU-018). validUntil va null mientras la asignación sigue vigente.
 */
public record AssignmentAdministrationWebResponse(String id, String userId, String tenantId, String applicationId,
                                                  String roleId, String validFrom, String validUntil) {
}
