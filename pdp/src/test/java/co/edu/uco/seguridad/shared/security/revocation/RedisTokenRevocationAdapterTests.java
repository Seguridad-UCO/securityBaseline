package co.edu.uco.seguridad.shared.security.revocation;

import co.edu.uco.seguridad.AbstractRedisIntegrationTest;
import co.edu.uco.seguridad.pdp.commons.model.UserId;
import co.edu.uco.seguridad.shared.security.RevocationRetentionProperties;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PLAN-HU-022.md §9 — cinco casos, contra una Redis real (no hay módulo oficial de Testcontainers
 * para Redis en este proyecto, ver {@link AbstractRedisIntegrationTest}). Se construye a mano, sin
 * contexto de Spring, mismo patrón que {@code SurrealRepositoryIntegrationTests}.
 */
class RedisTokenRevocationAdapterTests extends AbstractRedisIntegrationTest {

    private static ReactiveRedisTemplate<String, String> redis;

    @BeforeAll
    static void setUpClient() {
        RedisStandaloneConfiguration configuration = new RedisStandaloneConfiguration(REDIS.getHost(), REDIS.getMappedPort(6379));
        LettuceConnectionFactory connectionFactory = new LettuceConnectionFactory(configuration);
        connectionFactory.afterPropertiesSet();
        RedisSerializationContext<String, String> context = RedisSerializationContext
                .<String, String>newSerializationContext(new StringRedisSerializer())
                .build();
        redis = new ReactiveRedisTemplate<>(connectionFactory, context);
    }

    private static RedisTokenRevocationAdapter adapterWithRetention(Duration retention) {
        return new RedisTokenRevocationAdapter(redis, new RevocationRetentionProperties(retention));
    }

    @Test
    void is_not_revoked_before_any_revocation() {
        UserId subject = new UserId(UUID.randomUUID());
        RedisTokenRevocationAdapter adapter = adapterWithRetention(Duration.ofHours(1));

        StepVerifier.create(adapter.isRevoked(subject, Instant.parse("2026-09-16T00:00:00Z")))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    void is_revoked_when_issued_before_the_revoked_since_instant() {
        UserId subject = new UserId(UUID.randomUUID());
        RedisTokenRevocationAdapter adapter = adapterWithRetention(Duration.ofHours(1));
        Instant revokedSince = Instant.parse("2026-09-16T12:00:00Z");

        adapter.revokeAllSince(subject, revokedSince).block();

        StepVerifier.create(adapter.isRevoked(subject, revokedSince.minusSeconds(1)))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    void is_not_revoked_when_issued_after_the_revoked_since_instant() {
        UserId subject = new UserId(UUID.randomUUID());
        RedisTokenRevocationAdapter adapter = adapterWithRetention(Duration.ofHours(1));
        Instant revokedSince = Instant.parse("2026-09-16T12:00:00Z");

        adapter.revokeAllSince(subject, revokedSince).block();

        StepVerifier.create(adapter.isRevoked(subject, revokedSince.plusSeconds(1)))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    void the_most_recent_revocation_prevails_over_an_earlier_one() {
        UserId subject = new UserId(UUID.randomUUID());
        RedisTokenRevocationAdapter adapter = adapterWithRetention(Duration.ofHours(1));
        Instant earlier = Instant.parse("2026-09-16T10:00:00Z");
        Instant later = Instant.parse("2026-09-16T14:00:00Z");

        adapter.revokeAllSince(subject, earlier).block();
        adapter.revokeAllSince(subject, later).block();

        StepVerifier.create(adapter.isRevoked(subject, earlier.plusSeconds(1)))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    void the_revocation_key_has_a_ttl() {
        UserId subject = new UserId(UUID.randomUUID());
        RedisTokenRevocationAdapter adapter = adapterWithRetention(Duration.ofHours(1));

        adapter.revokeAllSince(subject, Instant.parse("2026-09-16T00:00:00Z")).block();

        StepVerifier.create(redis.getExpire("revoked-since:" + subject.value()))
                .assertNext(ttl -> assertThat(ttl).isPositive())
                .verifyComplete();
    }
}
