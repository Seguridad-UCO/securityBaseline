package co.edu.uco.seguridad.pep.ingress.infrastructure.adapter.primary.web.dto.response;

import java.net.URI;

/**
 * Proyección HTTP del resultado de registro.
 */
public record IntegrationWebResponse(String applicationId, String environment, String prefix, URI publicBaseUrl,
                                     String status) {
}
