package co.edu.uco.seguridad.pep.ingress.infrastructure.adapter.primary.web.dto.request.raw;

import java.net.URI;

/** Datos crudos del endpoint interno, incluidos path variables y credencial de transporte. */
public record RegisterIntegrationRawRequest(String applicationId, String environment, String authorization,
                                            URI backendUrl, String audience) {
}
