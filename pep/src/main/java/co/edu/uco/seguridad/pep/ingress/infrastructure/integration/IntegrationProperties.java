package co.edu.uco.seguridad.pep.ingress.infrastructure.integration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.nio.file.Path;
import java.util.List;

/**
 * Configuration for application self-registration. Tokens are BCrypt hashes, never plaintext.
 */
@ConfigurationProperties("pep.integration")
public record IntegrationProperties(boolean enabled, Path registryFile, URI publicBaseUrl, List<Credential> credentials) {

    public IntegrationProperties {
        credentials = credentials == null ? List.of() : List.copyOf(credentials);
        if (enabled) {
            if (registryFile == null || publicBaseUrl == null || publicBaseUrl.getHost() == null
                    || publicBaseUrl.getRawUserInfo() != null || publicBaseUrl.getRawQuery() != null
                    || publicBaseUrl.getRawFragment() != null || publicBaseUrl.getRawPath() != null
                    && !publicBaseUrl.getRawPath().isEmpty() || !"https".equals(publicBaseUrl.getScheme())) {
                throw new IllegalArgumentException("Integration registry file and HTTPS public base URL required");
            }
            for (Credential credential : credentials) {
                if (!identifier(credential.applicationId()) || !identifier(credential.environment())
                        || credential.tokenHash() == null || !credential.tokenHash().startsWith("$2")) {
                    throw new IllegalArgumentException("Integration credentials require identifiers and BCrypt token hashes");
                }
            }
        }
    }

    static boolean identifier(String value) {
        return value != null && value.matches("[A-Za-z0-9_.:-]{1,128}");
    }

    public record Credential(String applicationId, String environment, String tokenHash) {
    }
}
