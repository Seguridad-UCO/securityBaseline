package co.edu.uco.seguridad.shared.cache;

import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.RoleId;
import co.edu.uco.seguridad.pdp.commons.model.UserId;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementación de {@link DistributedCachePort} sobre Redis (HU-023, ADR-026). Clave
 * {@code active-roles:{userId}:{applicationId}} → valor: los {@code RoleId} unidos por coma, o
 * cadena vacía si el sujeto no tiene ningún rol activo (distinto de clave ausente = miss, ver
 * PLAN-HU-023.md §5). Las tres operaciones son fail-open: ninguna propaga un error de Redis (§7).
 */
public final class RedisDistributedCachePort implements DistributedCachePort {

    private static final Logger LOG = LoggerFactory.getLogger(RedisDistributedCachePort.class);
    private static final String KEY_PREFIX = "active-roles:";
    private static final String SEPARATOR = ",";

    private final ReactiveRedisTemplate<String, String> redis;
    private final ActiveRolesCacheRetentionProperties properties;

    public RedisDistributedCachePort(ReactiveRedisTemplate<String, String> redis, ActiveRolesCacheRetentionProperties properties) {
        this.redis = Objects.requireNonNull(redis, RequiredArgumentMessages.REACTIVE_REDIS_TEMPLATE);
        this.properties = Objects.requireNonNull(properties, RequiredArgumentMessages.ACTIVE_ROLES_CACHE_RETENTION_PROPERTIES);
    }

    @Override
    public Mono<Set<RoleId>> get(UserId subject, ApplicationId applicationId) {
        return redis.opsForValue().get(keyFor(subject, applicationId))
                .map(RedisDistributedCachePort::toRoleIds)
                .onErrorResume(cause -> {
                    logDegraded("get", subject, applicationId, cause);
                    return Mono.empty();
                });
    }

    @Override
    public Mono<Void> put(UserId subject, ApplicationId applicationId, Set<RoleId> roleIds) {
        return redis.opsForValue().set(keyFor(subject, applicationId), toValue(roleIds), properties.retention())
                .onErrorResume(cause -> {
                    logDegraded("put", subject, applicationId, cause);
                    return Mono.just(false);
                })
                .then();
    }

    @Override
    public Mono<Void> evict(UserId subject, ApplicationId applicationId) {
        return redis.opsForValue().delete(keyFor(subject, applicationId))
                .onErrorResume(cause -> {
                    logDegraded("evict", subject, applicationId, cause);
                    return Mono.just(false);
                })
                .then();
    }

    private static String keyFor(UserId subject, ApplicationId applicationId) {
        return KEY_PREFIX + subject.value() + ":" + applicationId.value();
    }

    private static String toValue(Set<RoleId> roleIds) {
        return roleIds.stream().map(roleId -> roleId.value().toString()).collect(Collectors.joining(SEPARATOR));
    }

    private static Set<RoleId> toRoleIds(String value) {
        if (value.isEmpty()) {
            return Set.of();
        }
        return Arrays.stream(value.split(SEPARATOR)).map(UUID::fromString).map(RoleId::new).collect(Collectors.toSet());
    }

    private static void logDegraded(String operation, UserId subject, ApplicationId applicationId, Throwable cause) {
        LOG.atWarn()
                .addKeyValue("event.name", "pdp.cache.active_roles.degraded")
                .addKeyValue("operation", operation)
                .addKeyValue("subject", subject.value())
                .addKeyValue("applicationId", applicationId.value())
                .setCause(cause)
                .log("Redis no respondió para la caché de roles activos — se degrada sin fallar (fail-open)");
    }
}
