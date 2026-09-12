package co.edu.uco.seguridad.pep.ingress.application.port.primary.dto.request;

import java.net.URI;

/** Comando de aplicación para registrar el backend de una integración PEP. */
public record RegisterIntegrationRequest(String applicationId, String environment, String bearerToken, URI backendUrl, String audience) {
}
