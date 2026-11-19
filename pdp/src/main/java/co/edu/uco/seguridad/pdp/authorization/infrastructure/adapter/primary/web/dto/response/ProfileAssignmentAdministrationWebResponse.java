package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.response;

import java.util.List;

/**
 * Respuesta plana, misma forma que {@code ProfileAssignmentWebResponse} (HU-019). validUntil va null mientras la asignación sigue vigente.
 */
public record ProfileAssignmentAdministrationWebResponse(String id, String userId, String tenantId,
                                                         String applicationId,
                                                         String profileId, List<String> generatedAssignmentIds,
                                                         String validFrom, String validUntil) {
}
