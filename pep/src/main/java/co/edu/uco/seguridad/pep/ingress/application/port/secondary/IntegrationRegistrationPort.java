package co.edu.uco.seguridad.pep.ingress.application.port.secondary;

import co.edu.uco.seguridad.pep.ingress.application.port.primary.dto.request.RegisterIntegrationRequest;
import co.edu.uco.seguridad.pep.ingress.application.port.primary.dto.response.RegisteredIntegrationResponse;
import reactor.core.publisher.Mono;

/** Puerto secundario para validar credenciales y persistir una ruta de integración. */
public interface IntegrationRegistrationPort {
    boolean enabled();
    boolean credentialMatches(String applicationId, String environment, String bearerToken);
    Mono<RegisteredIntegrationResponse> register(RegisterIntegrationRequest request);
}
