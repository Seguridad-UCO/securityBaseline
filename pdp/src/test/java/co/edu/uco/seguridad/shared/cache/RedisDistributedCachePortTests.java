package co.edu.uco.seguridad.shared.cache;

import co.edu.uco.seguridad.AbstractRedisIntegrationTest;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.RoleId;
import co.edu.uco.seguridad.pdp.commons.model.UserId;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PLAN-HU-023.md §9 — seis casos, contra una Redis real. Reutiliza {@link AbstractRedisIntegrationTest}
 * (creado en HU-022) por composición, no por herencia: se construye a mano, sin contexto de Spring,
 * mismo patrón que {@code RedisTokenRevocationAdapterTests}.
 */
class RedisDistributedCachePortTests {

    private static ReactiveRedisTemplate<String, String> redis;

    @BeforeAll
    static void setUpClient() {
        RedisStandaloneConfiguration configuration = new RedisStandaloneConfiguration(
                AbstractRedisIntegrationTest.REDIS.getHost(), AbstractRedisIntegrationTest.REDIS.getMappedPort(6379));
        LettuceConnectionFactory connectionFactory = new LettuceConnectionFactory(configuration);
        connectionFactory.afterPropertiesSet();
        redis = template(connectionFactory);
    }

    private static ReactiveRedisTemplate<String, String> template(LettuceConnectionFactory connectionFactory) {
        RedisSerializationContext<String, String> context = RedisSerializationContext
                .<String, String>newSerializationContext(new StringRedisSerializer())
                .build();
        return new ReactiveRedisTemplate<>(connectionFactory, context);
    }

    private static RedisDistributedCachePort adapterWithRetention(Duration retention) {
        return new RedisDistributedCachePort(redis, new ActiveRolesCacheRetentionProperties(retention));
    }

    @Test
    void is_empty_before_any_put() {
        UserId subject = new UserId(UUID.randomUUID());
        ApplicationId application = new ApplicationId(UUID.randomUUID());
        RedisDistributedCachePort adapter = adapterWithRetention(Duration.ofSeconds(60));

        StepVerifier.create(adapter.get(subject, application)).verifyComplete();
    }

    @Test
    void put_then_get_returns_the_same_set() {
        UserId subject = new UserId(UUID.randomUUID());
        ApplicationId application = new ApplicationId(UUID.randomUUID());
        RoleId role = new RoleId(UUID.randomUUID());
        RedisDistributedCachePort adapter = adapterWithRetention(Duration.ofSeconds(60));

        adapter.put(subject, application, Set.of(role)).block();

        StepVerifier.create(adapter.get(subject, application))
                .assertNext(roleIds -> assertThat(roleIds).containsExactly(role))
                .verifyComplete();
    }

    @Test
    void put_with_an_empty_set_then_get_returns_an_empty_set_not_a_miss() {
        UserId subject = new UserId(UUID.randomUUID());
        ApplicationId application = new ApplicationId(UUID.randomUUID());
        RedisDistributedCachePort adapter = adapterWithRetention(Duration.ofSeconds(60));

        adapter.put(subject, application, Set.of()).block();

        StepVerifier.create(adapter.get(subject, application))
                .assertNext(roleIds -> assertThat(roleIds).isEmpty())
                .verifyComplete();
    }

    @Test
    void evict_after_put_makes_get_empty_again() {
        UserId subject = new UserId(UUID.randomUUID());
        ApplicationId application = new ApplicationId(UUID.randomUUID());
        RoleId role = new RoleId(UUID.randomUUID());
        RedisDistributedCachePort adapter = adapterWithRetention(Duration.ofSeconds(60));

        adapter.put(subject, application, Set.of(role)).block();
        adapter.evict(subject, application).block();

        StepVerifier.create(adapter.get(subject, application)).verifyComplete();
    }

    @Test
    void the_cache_key_has_a_ttl_after_put() {
        UserId subject = new UserId(UUID.randomUUID());
        ApplicationId application = new ApplicationId(UUID.randomUUID());
        RoleId role = new RoleId(UUID.randomUUID());
        RedisDistributedCachePort adapter = adapterWithRetention(Duration.ofSeconds(60));

        adapter.put(subject, application, Set.of(role)).block();

        StepVerifier.create(redis.getExpire("active-roles:" + subject.value() + ":" + application.value()))
                .assertNext(ttl -> assertThat(ttl).isPositive())
                .verifyComplete();
    }

    @Test
    void get_put_and_evict_never_propagate_an_error_when_redis_is_unreachable() {
        UserId subject = new UserId(UUID.randomUUID());
        ApplicationId application = new ApplicationId(UUID.randomUUID());
        RedisStandaloneConfiguration unreachable = new RedisStandaloneConfiguration("127.0.0.1", 1);
        LettuceClientConfiguration clientConfiguration = LettuceClientConfiguration.builder()
                .commandTimeout(Duration.ofSeconds(2))
                .build();
        LettuceConnectionFactory connectionFactory = new LettuceConnectionFactory(unreachable, clientConfiguration);
        connectionFactory.afterPropertiesSet();
        RedisDistributedCachePort adapter = new RedisDistributedCachePort(
                template(connectionFactory), new ActiveRolesCacheRetentionProperties(Duration.ofSeconds(60)));

        StepVerifier.create(adapter.get(subject, application)).verifyComplete();
        StepVerifier.create(adapter.put(subject, application, Set.of())).verifyComplete();
        StepVerifier.create(adapter.evict(subject, application)).verifyComplete();
    }
}
