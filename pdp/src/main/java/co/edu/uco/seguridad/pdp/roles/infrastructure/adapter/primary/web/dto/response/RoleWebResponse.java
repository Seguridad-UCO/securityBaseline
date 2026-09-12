package co.edu.uco.seguridad.pdp.roles.infrastructure.adapter.primary.web.dto.response;

import java.util.List;

/** Respuesta plana. tenantId y applicationId van nulos cuando el alcance no los tiene. */
public record RoleWebResponse(String id, String name, String scope, String tenantId, String applicationId,
        List<String> resourceIds, String registeredAt) {
}
