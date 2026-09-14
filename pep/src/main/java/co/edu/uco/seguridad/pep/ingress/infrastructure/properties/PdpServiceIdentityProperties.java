package co.edu.uco.seguridad.pep.ingress.infrastructure.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.time.Duration;

/** Credenciales OAuth2 de máquina del PEP; el access token nunca se persiste en configuración. */
@ConfigurationProperties("pep.pdp.service-identity")
public record PdpServiceIdentityProperties(URI tokenUri, String clientId, String clientSecret,
                                           Duration timeout, Duration refreshSkew) {
    public PdpServiceIdentityProperties {
        timeout = timeout == null ? Duration.ofSeconds(3) : timeout;
        refreshSkew = refreshSkew == null ? Duration.ofSeconds(30) : refreshSkew;
        if (timeout.isNegative() || timeout.isZero() || refreshSkew.isNegative())
            throw new IllegalArgumentException("PEP service identity timeouts must be positive");
    }

    public boolean configured() {
        return tokenUri != null && tokenUri.getHost() != null && tokenUri.getRawUserInfo() == null
                && tokenUri.getRawFragment() == null && clientId != null && !clientId.isBlank()
                && clientSecret != null && !clientSecret.isBlank();
    }
}
