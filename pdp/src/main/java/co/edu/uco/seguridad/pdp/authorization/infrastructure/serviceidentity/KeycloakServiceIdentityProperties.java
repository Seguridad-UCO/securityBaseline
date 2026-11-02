package co.edu.uco.seguridad.pdp.authorization.infrastructure.serviceidentity;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.time.Duration;

/**
 * Credencial de aprovisionamiento exclusiva del PDP; no es la credencial de una aplicación.
 */
@ConfigurationProperties("pdp.service-identities.keycloak")
public record KeycloakServiceIdentityProperties(boolean enabled, URI baseUrl, URI tokenUri, String realm,
                                                String clientId, String clientSecret, String evidenceAudience,
                                                Duration timeout) {
}
