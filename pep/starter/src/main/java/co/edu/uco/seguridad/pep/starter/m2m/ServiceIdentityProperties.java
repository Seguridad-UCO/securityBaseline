package co.edu.uco.seguridad.pep.starter.m2m;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.time.Duration;

/** Propiedades de una identidad M2M concreta, normalmente inyectadas por el gestor de secretos. */
@ConfigurationProperties("security.pep.service-identity")
public record ServiceIdentityProperties(URI tokenUri, String clientId, String clientSecret,
                                        Duration timeout, Duration refreshSkew) {
    public ServiceIdentityProperties {
        timeout = timeout == null ? Duration.ofSeconds(3) : timeout;
        refreshSkew = refreshSkew == null ? Duration.ofSeconds(30) : refreshSkew;
    }

    public boolean configured() {
        return tokenUri != null && tokenUri.getHost() != null && tokenUri.getRawUserInfo() == null
                && tokenUri.getRawFragment() == null && clientId != null && !clientId.isBlank()
                && clientSecret != null && !clientSecret.isBlank();
    }
}
