package co.edu.uco.seguridad.shared.security;

import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.Objects;

/**
 * TTL de la clave {@code revoked-since:{userId}} en Redis (HU-022, ADR-026).
 *
 * @param retention tiempo tras el cual la clave de revocación puede vencer sin riesgo: cualquier
 *                   token con {@code issuedAt} anterior ya habría expirado por sí solo.
 */
@ConfigurationProperties(prefix = "pdp.security.revocation")
public record RevocationRetentionProperties(Duration retention) {

    public RevocationRetentionProperties {
        Objects.requireNonNull(retention, RequiredArgumentMessages.REVOCATION_RETENTION);
    }
}
