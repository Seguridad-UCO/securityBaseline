package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.request.raw;

import java.util.List;

/** Contexto de aplicación y conjunto completo de recursos para una operación masiva de rol. */
public record BatchRoleResourcesRawRequest(String applicationId, List<String> resourceIds) {
    public BatchRoleResourcesRawRequest {
        resourceIds = resourceIds == null ? List.of() : List.copyOf(resourceIds);
    }
}
