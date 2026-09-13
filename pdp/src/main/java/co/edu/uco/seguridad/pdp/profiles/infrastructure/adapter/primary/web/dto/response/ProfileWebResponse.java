package co.edu.uco.seguridad.pdp.profiles.infrastructure.adapter.primary.web.dto.response;

import java.util.List;

/** Respuesta plana. tenantId y applicationId van nulos cuando el alcance no los tiene. */
public record ProfileWebResponse(String id, String name, String scope, String tenantId, String applicationId,
        List<String> roleIds, String registeredAt) {
}
