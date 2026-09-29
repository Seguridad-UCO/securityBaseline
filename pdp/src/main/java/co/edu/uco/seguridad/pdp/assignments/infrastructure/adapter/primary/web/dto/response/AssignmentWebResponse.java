package co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.primary.web.dto.response;

/**
 * Respuesta plana. validUntil va null mientras la asignación sigue vigente.
 */
public record AssignmentWebResponse(String id, String userId, String tenantId, String applicationId, String roleId,
                                    String validFrom, String validUntil) {
}
