package co.edu.uco.seguridad.pep.enforcement.infrastructure.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;

/**
 * Configuración del adaptador HTTP secundario que consulta al PDP.
 */
@ConfigurationProperties("pep.pdp")
public record PdpClientProperties(URI baseUrl, boolean allowInsecureHttp, Duration connectTimeout,
                                  Duration timeout, int maxResponseBytes, Path caCertificate, Path clientCertificate,
                                  Path clientKey) {
    public PdpClientProperties {
        if (baseUrl == null || baseUrl.getHost() == null || baseUrl.getRawUserInfo() != null
                || baseUrl.getRawQuery() != null || baseUrl.getRawFragment() != null
                || (baseUrl.getRawPath() != null && !baseUrl.getRawPath().isEmpty())
                || (!"https".equals(baseUrl.getScheme()) && !(allowInsecureHttp && "http".equals(baseUrl.getScheme())))) {
            throw new IllegalArgumentException("PDP URL must be an explicit HTTPS origin");
        }
        if (!allowInsecureHttp && (clientCertificate == null || clientKey == null || caCertificate == null)) {
            throw new IllegalArgumentException("PDP mTLS certificate, key and CA are required outside development");
        }
        if ((clientCertificate == null) != (clientKey == null))
            throw new IllegalArgumentException("Incomplete PDP client certificate");
        if (connectTimeout == null || connectTimeout.isNegative() || connectTimeout.isZero()
                || timeout == null || timeout.isNegative() || timeout.isZero() || maxResponseBytes < 1) {
            throw new IllegalArgumentException("PDP timeouts and response limit must be positive");
        }
    }
}
