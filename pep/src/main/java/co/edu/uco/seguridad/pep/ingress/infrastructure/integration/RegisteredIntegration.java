package co.edu.uco.seguridad.pep.ingress.infrastructure.integration;

import java.net.URI;

/**
 * Persisted technical route. Policy data deliberately remains in the PDP.
 */
public record RegisteredIntegration(String applicationId, String environment, String prefix, URI backendUrl,
                                    String audience) {
}
