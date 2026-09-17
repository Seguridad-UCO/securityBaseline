package co.edu.uco.seguridad.shared.security.revocation;

import co.edu.uco.seguridad.pdp.commons.model.UserId;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import co.edu.uco.seguridad.shared.security.RevocationRetentionProperties;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Objects;

/**
 * Implementación de {@link TokenRevocationPort} sobre Redis (HU-022, ADR-026). Clave
 * {@code revoked-since:{userId}} → valor: instante ISO-8601, con TTL {@code properties.retention()};
 * {@code SET} siempre sobrescribe (nunca acumula), así que una revocación posterior deja prevalecer
 * el instante más reciente (PLAN-HU-022.md §5).
 */
public final class RedisTokenRevocationAdapter implements TokenRevocationPort {

    private static final String KEY_PREFIX = "revoked-since:";

    private final ReactiveRedisTemplate<String, String> redis;
    private final RevocationRetentionProperties properties;

    public RedisTokenRevocationAdapter(ReactiveRedisTemplate<String, String> redis, RevocationRetentionProperties properties) {
        this.redis = Objects.requireNonNull(redis, RequiredArgumentMessages.REACTIVE_REDIS_TEMPLATE);
        this.properties = Objects.requireNonNull(properties, RequiredArgumentMessages.REVOCATION_RETENTION_PROPERTIES);
    }

    @Override
    public Mono<Void> revokeAllSince(UserId subject, Instant since) {
        return redis.opsForValue().set(keyFor(subject), since.toString(), properties.retention()).then();
    }

    @Override
    public Mono<Boolean> isRevoked(UserId subject, Instant issuedAt) {
        return redis.opsForValue().get(keyFor(subject))
                .map(Instant::parse)
                .map(revokedSince -> !issuedAt.isAfter(revokedSince))
                .defaultIfEmpty(false);
    }

    private static String keyFor(UserId subject) {
        return KEY_PREFIX + subject.value();
    }
}
