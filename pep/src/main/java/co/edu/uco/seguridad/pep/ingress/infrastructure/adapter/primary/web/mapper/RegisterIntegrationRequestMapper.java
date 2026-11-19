package co.edu.uco.seguridad.pep.ingress.infrastructure.adapter.primary.web.mapper;

import co.edu.uco.seguridad.pep.ingress.application.port.primary.dto.request.RegisterIntegrationRequest;
import co.edu.uco.seguridad.pep.ingress.infrastructure.adapter.primary.web.dto.request.raw.RegisterIntegrationRawRequest;

/**
 * Traduce el contrato HTTP sin propagar sus anotaciones o tipos hacia aplicación.
 */
public final class RegisterIntegrationRequestMapper {
    private RegisterIntegrationRequestMapper() {
    }

    public static RegisterIntegrationRequest toRequest(RegisterIntegrationRawRequest raw) {
        return new RegisterIntegrationRequest(raw.applicationId(), raw.environment(), bearer(raw.authorization()),
                raw.backendUrl(), raw.audience());
    }

    private static String bearer(String authorization) {
        return authorization != null && authorization.startsWith("Bearer ") && authorization.length() > 7
                ? authorization.substring(7) : null;
    }
}
