package co.edu.uco.seguridad.shared.security.revocation;

import co.edu.uco.seguridad.pdp.commons.model.UserId;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import co.edu.uco.seguridad.shared.security.RevocationRetentionProperties;
import co.edu.uco.seguridad.shared.port.TimeProvider;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import reactor.core.publisher.Mono;

import java.time.Duration;
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
    private static final String TOKEN_KEY_PREFIX = "revoked-token:";

    private final ReactiveRedisTemplate<String, String> redis;
    private final RevocationRetentionProperties properties;
    private final TimeProvider timeProvider;

    public RedisTokenRevocationAdapter(ReactiveRedisTemplate<String, String> redis,
                                       RevocationRetentionProperties properties,
                                       TimeProvider timeProvider) {
        this.redis = Objects.requireNonNull(redis, RequiredArgumentMessages.REACTIVE_REDIS_TEMPLATE);
        this.properties = Objects.requireNonNull(properties, RequiredArgumentMessages.REVOCATION_RETENTION_PROPERTIES);
        this.timeProvider = Objects.requireNonNull(timeProvider, "timeProvider");
    }

    @Override
    public Mono<Void> revokeAllSince(UserId subject, Instant since) {
        return redis.opsForValue().set(keyFor(subject), since.toString(), properties.retention()).then();
    }

    @Override
    public Mono<Void> revokeToken(UserId subject, String tokenId, Instant expiresAt) {
        Objects.requireNonNull(subject, "subject");
        Objects.requireNonNull(tokenId, "tokenId");
        Objects.requireNonNull(expiresAt, "expiresAt");
        Duration ttl = Duration.between(timeProvider.now(), expiresAt);
        if (ttl.isNegative() || ttl.isZero()) {
            return Mono.empty();
        }
        return redis.opsForValue().set(tokenKeyFor(subject, tokenId), "1", ttl).then();
    }

    @Override
    public Mono<Boolean> isRevoked(UserId subject, Instant issuedAt) {
        return redis.opsForValue().get(keyFor(subject))
                .map(Instant::parse)
                .map(revokedSince -> !issuedAt.isAfter(revokedSince))
                .defaultIfEmpty(false);
    }

    @Override
    public Mono<Boolean> isTokenRevoked(UserId subject, String tokenId) {
        return redis.opsForValue().get(tokenKeyFor(subject, tokenId))
                .hasElement();
    }

    private static String keyFor(UserId subject) {
        return KEY_PREFIX + subject.value();
    }

    private static String tokenKeyFor(UserId subject, String tokenId) {
        return TOKEN_KEY_PREFIX + subject.value() + ":" + tokenId;
    }
}
