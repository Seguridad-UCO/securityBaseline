package co.edu.uco.seguridad.shared.auth.infrastructure;

import co.edu.uco.seguridad.shared.auth.port.RefreshTokenStorePort;
import co.edu.uco.seguridad.shared.port.TimeProvider;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

public final class RedisRefreshTokenStoreAdapter implements RefreshTokenStorePort {

    private static final String KEY_PREFIX = "oidc-refresh-token:";

    private final ReactiveRedisTemplate<String, String> redis;
    private final TimeProvider timeProvider;

    public RedisRefreshTokenStoreAdapter(ReactiveRedisTemplate<String, String> redis, TimeProvider timeProvider) {
        this.redis = Objects.requireNonNull(redis, "redis");
        this.timeProvider = Objects.requireNonNull(timeProvider, "timeProvider");
    }

    @Override
    public Mono<Void> store(String sessionId, String refreshToken, Instant expiresAt) {
        requireText(sessionId, "sessionId");
        requireText(refreshToken, "refreshToken");
        Objects.requireNonNull(expiresAt, "expiresAt");
        Duration ttl = Duration.between(timeProvider.now(), expiresAt);
        if (ttl.isNegative() || ttl.isZero()) {
            return Mono.error(new IllegalArgumentException("refresh token expirado"));
        }
        return redis.opsForValue().set(keyFor(sessionId), refreshToken, ttl).then();
    }

    @Override
    public Mono<String> find(String sessionId) {
        requireText(sessionId, "sessionId");
        return redis.opsForValue().get(keyFor(sessionId));
    }

    @Override
    public Mono<Void> delete(String sessionId) {
        requireText(sessionId, "sessionId");
        return redis.delete(keyFor(sessionId)).then();
    }

    private static String keyFor(String sessionId) {
        return KEY_PREFIX + sessionId;
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " es obligatorio");
        }
    }
}
