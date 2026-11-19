package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.mapper;

import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.response.AdministeredResourceWebResponse;
import co.edu.uco.seguridad.pdp.resources.application.primaryport.response.RegisteredProtectedResourceResponse;

/**
 * {@code RegisteredProtectedResourceResponse} (value objects) a {@code AdministeredResourceWebResponse}
 * (plana) — idéntico a {@code resources...ProtectedResourceResponseMapper}.
 */
public final class AdministeredResourceResponseMapper {

    private AdministeredResourceResponseMapper() {
    }

    public static AdministeredResourceWebResponse toResponse(RegisteredProtectedResourceResponse response) {
        return new AdministeredResourceWebResponse(response.id().value().toString(),
                response.applicationId().value().toString(), response.tenantId().value(),
                response.path().value(), response.method().name(), response.registeredAt());
    }
}
