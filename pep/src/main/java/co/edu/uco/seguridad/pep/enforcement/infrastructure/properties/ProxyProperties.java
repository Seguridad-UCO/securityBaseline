package co.edu.uco.seguridad.pep.enforcement.infrastructure.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties("pep.proxy")
public record ProxyProperties(Duration connectTimeout, Duration timeout, long maxBodyBytes) {

    public ProxyProperties {
        if (connectTimeout == null || connectTimeout.isNegative() || connectTimeout.isZero()
                || timeout == null || timeout.isNegative() || timeout.isZero() || maxBodyBytes < 1) {
            throw new IllegalArgumentException("Proxy limits must be positive");
        }
    }

}

