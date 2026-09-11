package co.edu.uco.seguridad.pep.ingress.application.port.primary.dto.response;

import java.net.URI;

/** Resultado de aplicación del registro de una integración. */
public record RegisteredIntegrationResponse(String applicationId, String environment, String prefix, URI publicBaseUrl, String status) {
}
