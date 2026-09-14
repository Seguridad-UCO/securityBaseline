package co.edu.uco.seguridad.pep.ingress.infrastructure.integration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.nio.file.Path;

/**
 * Configuration for application self-registration. Credential validation belongs to the PDP.
 */
@ConfigurationProperties("pep.integration")
public record IntegrationProperties(boolean enabled, Path registryFile, URI publicBaseUrl) {

    public IntegrationProperties {
        if (enabled) {
            if (registryFile == null || publicBaseUrl == null || publicBaseUrl.getHost() == null
                    || publicBaseUrl.getRawUserInfo() != null || publicBaseUrl.getRawQuery() != null
                    || publicBaseUrl.getRawFragment() != null || publicBaseUrl.getRawPath() != null
                    && !publicBaseUrl.getRawPath().isEmpty() || !"https".equals(publicBaseUrl.getScheme())) {
                throw new IllegalArgumentException("Integration registry file and HTTPS public base URL are required");
            }
        }
    }

    static boolean identifier(String value) {
        return value != null && value.matches("[A-Za-z0-9_.:-]{1,128}");
    }
}
