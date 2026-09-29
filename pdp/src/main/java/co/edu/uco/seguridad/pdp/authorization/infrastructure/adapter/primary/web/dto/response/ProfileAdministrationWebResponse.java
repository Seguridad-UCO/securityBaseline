package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.response;

import java.util.List;

/**
 * Respuesta plana, misma forma que {@code ProfileWebResponse} (HU-019). tenantId y applicationId van nulos cuando el alcance no los tiene.
 */
public record ProfileAdministrationWebResponse(String id, String name, String scope, String tenantId,
                                               String applicationId, List<String> roleIds, String registeredAt) {
}
