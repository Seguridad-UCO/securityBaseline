package co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.primary.web.dto.response;

import java.util.List;

/**
 * Respuesta plana. validUntil va null mientras la asignación sigue vigente.
 */
public record ProfileAssignmentWebResponse(String id, String userId, String tenantId, String applicationId,
                                           String profileId, List<String> generatedAssignmentIds, String validFrom,
                                           String validUntil) {
}
