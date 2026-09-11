package co.edu.uco.seguridad.pep.ingress.infrastructure.adapter.primary.web.mapper;

import co.edu.uco.seguridad.pep.ingress.application.port.primary.dto.response.RegisteredIntegrationResponse;
import co.edu.uco.seguridad.pep.ingress.infrastructure.adapter.primary.web.dto.response.IntegrationWebResponse;

public final class IntegrationResponseMapper {
    private IntegrationResponseMapper() {
    }

    public static IntegrationWebResponse toResponse(RegisteredIntegrationResponse response) {
        return new IntegrationWebResponse(response.applicationId(), response.environment(), response.prefix(),
                response.publicBaseUrl(), response.status());
    }
}
