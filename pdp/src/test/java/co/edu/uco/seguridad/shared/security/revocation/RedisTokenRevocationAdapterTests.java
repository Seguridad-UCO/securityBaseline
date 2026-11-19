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
        return new RedisTokenRevocationAdapter(redis, new RevocationRetentionProperties(retention), Instant::now);
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

    @Test
    void revokes_only_the_exact_token_until_its_expiration() {
        UserId subject = new UserId(UUID.randomUUID());
        RedisTokenRevocationAdapter adapter = adapterWithRetention(Duration.ofHours(1));
        String tokenId = UUID.randomUUID().toString();

        adapter.revokeToken(subject, tokenId, Instant.now().plusSeconds(300)).block();

        StepVerifier.create(adapter.isTokenRevoked(subject, tokenId))
                .expectNext(true)
                .verifyComplete();
        StepVerifier.create(adapter.isTokenRevoked(subject, "different-token"))
                .expectNext(false)
                .verifyComplete();
    }

    // ---- HU-028 / ADR-034: revocación acotada por cliente (azp) ----

    @Test
    void client_is_not_revoked_when_there_is_no_client_revocation() {
        UserId subject = new UserId(UUID.randomUUID());
        RedisTokenRevocationAdapter adapter = adapterWithRetention(Duration.ofHours(1));

        StepVerifier.create(adapter.isClientRevoked(subject, "app-a", Instant.parse("2026-10-28T10:00:00Z")))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    void client_is_revoked_when_issued_before_or_at_the_revoked_since_instant() {
        UserId subject = new UserId(UUID.randomUUID());
        RedisTokenRevocationAdapter adapter = adapterWithRetention(Duration.ofHours(1));
        Instant revokedSince = Instant.parse("2026-10-28T12:00:00Z");

        StepVerifier.create(adapter.revokeClientSince(subject, "app-a", revokedSince)).verifyComplete();

        StepVerifier.create(adapter.isClientRevoked(subject, "app-a", revokedSince.minusSeconds(1)))
                .expectNext(true)
                .verifyComplete();
        StepVerifier.create(adapter.isClientRevoked(subject, "app-a", revokedSince))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    void client_is_not_revoked_when_issued_after_the_revoked_since_instant() {
        UserId subject = new UserId(UUID.randomUUID());
        RedisTokenRevocationAdapter adapter = adapterWithRetention(Duration.ofHours(1));
        Instant revokedSince = Instant.parse("2026-10-28T12:00:00Z");

        StepVerifier.create(adapter.revokeClientSince(subject, "app-a", revokedSince)).verifyComplete();

        StepVerifier.create(adapter.isClientRevoked(subject, "app-a", revokedSince.plusSeconds(1)))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    void client_revocation_does_not_affect_another_client_of_the_same_subject() {
        UserId subject = new UserId(UUID.randomUUID());
        RedisTokenRevocationAdapter adapter = adapterWithRetention(Duration.ofHours(1));
        Instant revokedSince = Instant.parse("2026-10-28T12:00:00Z");

        StepVerifier.create(adapter.revokeClientSince(subject, "app-a", revokedSince)).verifyComplete();

        StepVerifier.create(adapter.isClientRevoked(subject, "app-b", revokedSince.minusSeconds(60)))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    void client_revocation_does_not_affect_another_subject_of_the_same_client() {
        UserId subject = new UserId(UUID.randomUUID());
        UserId otherSubject = new UserId(UUID.randomUUID());
        RedisTokenRevocationAdapter adapter = adapterWithRetention(Duration.ofHours(1));
        Instant revokedSince = Instant.parse("2026-10-28T12:00:00Z");

        StepVerifier.create(adapter.revokeClientSince(subject, "app-a", revokedSince)).verifyComplete();

        StepVerifier.create(adapter.isClientRevoked(otherSubject, "app-a", revokedSince.minusSeconds(60)))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    void a_second_client_revocation_overwrites_the_first() {
        UserId subject = new UserId(UUID.randomUUID());
        RedisTokenRevocationAdapter adapter = adapterWithRetention(Duration.ofHours(1));
        Instant first = Instant.parse("2026-10-28T10:00:00Z");
        Instant second = Instant.parse("2026-10-28T14:00:00Z");

        StepVerifier.create(adapter.revokeClientSince(subject, "app-a", second)).verifyComplete();
        StepVerifier.create(adapter.revokeClientSince(subject, "app-a", first)).verifyComplete();

        StepVerifier.create(adapter.isClientRevoked(subject, "app-a", first.plusSeconds(1)))
                .expectNext(false)
                .verifyComplete();
        StepVerifier.create(adapter.isClientRevoked(subject, "app-a", first))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    void the_client_revocation_key_lives_for_the_configured_retention() {
        UserId subject = new UserId(UUID.randomUUID());
        RedisTokenRevocationAdapter adapter = adapterWithRetention(Duration.ofHours(1));

        StepVerifier.create(adapter.revokeClientSince(subject, "app-a", Instant.parse("2026-10-28T12:00:00Z")))
                .verifyComplete();

        StepVerifier.create(redis.getExpire("revoked-client-since:" + subject.value() + ":app-a"))
                .assertNext(ttl -> assertThat(ttl).isPositive().isLessThanOrEqualTo(Duration.ofHours(1))
                        .isGreaterThan(Duration.ofMinutes(59)))
                .verifyComplete();
    }

    @Test
    void client_revocation_is_independent_from_the_subject_wide_revocation() {
        UserId subject = new UserId(UUID.randomUUID());
        RedisTokenRevocationAdapter adapter = adapterWithRetention(Duration.ofHours(1));
        Instant revokedSince = Instant.parse("2026-10-28T12:00:00Z");
        Instant issuedAt = revokedSince.minusSeconds(60);

        StepVerifier.create(adapter.revokeClientSince(subject, "app-a", revokedSince)).verifyComplete();
        StepVerifier.create(adapter.isRevoked(subject, issuedAt)).expectNext(false).verifyComplete();

        StepVerifier.create(adapter.revokeAllSince(subject, revokedSince)).verifyComplete();
        StepVerifier.create(adapter.isClientRevoked(subject, "app-b", issuedAt)).expectNext(false).verifyComplete();
    }

    @Test
    void exact_token_revocation_key_expires_with_the_token() {
        UserId subject = new UserId(UUID.randomUUID());
        RedisTokenRevocationAdapter adapter = adapterWithRetention(Duration.ofHours(1));
        String tokenId = UUID.randomUUID().toString();

        adapter.revokeToken(subject, tokenId, Instant.now().plusSeconds(300)).block();

        StepVerifier.create(redis.getExpire("revoked-token:" + subject.value() + ":" + tokenId))
                .assertNext(ttl -> assertThat(ttl).isPositive().isLessThanOrEqualTo(Duration.ofSeconds(300)))
                .verifyComplete();
    }
}
