package co.edu.uco.seguridad.shared.cache;

import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.Objects;

/**
 * TTL de respaldo de la clave {@code active-roles:{userId}:{applicationId}} en Redis (HU-023,
 * ADR-026).
 *
 * @param retention red de seguridad: acota cuánto puede durar una entrada obsoleta si un
 *                  {@code evict()} llegara a fallar silenciosamente contra Redis (el puerto es
 *                  fail-open también en escritura, ver {@link DistributedCachePort}).
 */
@ConfigurationProperties(prefix = "pdp.cache.active-roles")
public record ActiveRolesCacheRetentionProperties(Duration retention) {

    public ActiveRolesCacheRetentionProperties {
        Objects.requireNonNull(retention, RequiredArgumentMessages.ACTIVE_ROLES_CACHE_RETENTION);
    }
}
