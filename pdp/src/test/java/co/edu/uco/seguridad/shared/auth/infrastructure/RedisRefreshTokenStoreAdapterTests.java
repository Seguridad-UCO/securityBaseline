package co.edu.uco.seguridad.shared.auth.infrastructure;

import co.edu.uco.seguridad.AbstractRedisIntegrationTest;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** El refresh token vive en Redis con TTL igual a su vigencia real; se prueba contra una Redis real. */
class RedisRefreshTokenStoreAdapterTests extends AbstractRedisIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-10-28T12:00:00Z");

    private static ReactiveRedisTemplate<String, String> redis;

    @BeforeAll
    static void setUpClient() {
        LettuceConnectionFactory factory = new LettuceConnectionFactory(
                new RedisStandaloneConfiguration(REDIS.getHost(), REDIS.getMappedPort(6379)));
        factory.afterPropertiesSet();
        redis = new ReactiveRedisTemplate<>(factory, RedisSerializationContext
                .<String, String>newSerializationContext(new StringRedisSerializer()).build());
    }

    private static RedisRefreshTokenStoreAdapter adapter() {
        return new RedisRefreshTokenStoreAdapter(redis, () -> NOW);
    }

    private static String session() {
        return "sid-" + UUID.randomUUID();
    }

    @Test
    void stores_finds_and_deletes_a_refresh_token() {
        String sid = session();

        StepVerifier.create(adapter().store(sid, "refresh-1", NOW.plusSeconds(300))).verifyComplete();
        StepVerifier.create(adapter().find(sid)).expectNext("refresh-1").verifyComplete();
        StepVerifier.create(adapter().delete(sid)).verifyComplete();
        StepVerifier.create(adapter().find(sid)).verifyComplete();
    }

    @Test
    void the_stored_key_expires_with_the_refresh_token() {
        String sid = session();

        StepVerifier.create(adapter().store(sid, "refresh-1", NOW.plusSeconds(120)).then(
                        redis.getExpire("oidc-refresh-token:" + sid)))
                .assertNext(ttl -> assertThat(ttl).isBetween(Duration.ofSeconds(100), Duration.ofSeconds(120)))
                .verifyComplete();
    }

    @Test
    void refuses_to_store_an_already_expired_token() {
        StepVerifier.create(adapter().store(session(), "refresh-1", NOW))
                .expectErrorSatisfies(error -> assertThat(error)
                        .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("expirado"))
                .verify();
    }

    @Test
    void requires_text_arguments() {
        assertThatThrownBy(() -> adapter().store(" ", "t", NOW.plusSeconds(5)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("sessionId");
        assertThatThrownBy(() -> adapter().store("s", "", NOW.plusSeconds(5)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("refreshToken");
        assertThatThrownBy(() -> adapter().find(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> adapter().delete("")).isInstanceOf(IllegalArgumentException.class);
    }
}
