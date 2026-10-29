package co.edu.uco.seguridad.shared.config;

import co.edu.uco.seguridad.shared.cache.ActiveRolesCacheRetentionProperties;
import co.edu.uco.seguridad.shared.cache.DistributedCachePort;
import co.edu.uco.seguridad.shared.cache.ObservedDistributedCachePort;
import co.edu.uco.seguridad.shared.cache.RedisDistributedCachePort;
import co.edu.uco.seguridad.shared.auth.infrastructure.RedisRefreshTokenStoreAdapter;
import co.edu.uco.seguridad.shared.auth.port.RefreshTokenStorePort;
import co.edu.uco.seguridad.shared.security.RevocationRetentionProperties;
import co.edu.uco.seguridad.shared.security.revocation.RedisTokenRevocationAdapter;
import co.edu.uco.seguridad.shared.security.revocation.TokenRevocationPort;
import co.edu.uco.seguridad.shared.port.TimeProvider;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Cableado de Redis (HU-022/HU-023, ADR-026). {@code ReactiveRedisConnectionFactory} lo
 * autoconfigura Boot a partir de {@code spring.data.redis.*} (Lettuce reactivo por defecto) — aquí
 * solo se declaran el template tipado y los dos puertos secundarios que ADR-026 mantiene separados
 * (revocación, HU-022; caché de roles activos, HU-023), mismo patrón que {@code SurrealDbConfiguration}.
 *
 * <p>{@code @Primary} en el template: Boot ya autoconfigura {@code reactiveStringRedisTemplate}
 * (mismo tipo genérico {@code ReactiveRedisTemplate<String, String>}) — sin esto, cualquier inyección
 * de ese tipo es ambigua y el contexto de Spring falla al arrancar con "expected single matching
 * bean but found 2".</p>
 */
@Configuration
@EnableConfigurationProperties({RevocationRetentionProperties.class, ActiveRolesCacheRetentionProperties.class})
public class RedisConfiguration {

    @Bean
    @Primary
    ReactiveRedisTemplate<String, String> reactiveRedisTemplate(ReactiveRedisConnectionFactory connectionFactory) {
        RedisSerializationContext<String, String> context = RedisSerializationContext
                .<String, String>newSerializationContext(new StringRedisSerializer())
                .build();
        return new ReactiveRedisTemplate<>(connectionFactory, context);
    }

    @Bean
    TokenRevocationPort tokenRevocationPort(ReactiveRedisTemplate<String, String> redis,
                                            RevocationRetentionProperties properties,
                                            TimeProvider timeProvider) {
        return new RedisTokenRevocationAdapter(redis, properties, timeProvider);
    }

    @Bean
    RefreshTokenStorePort refreshTokenStorePort(ReactiveRedisTemplate<String, String> redis, TimeProvider timeProvider) {
        return new RedisRefreshTokenStoreAdapter(redis, timeProvider);
    }

    @Bean
    DistributedCachePort distributedCachePort(ReactiveRedisTemplate<String, String> redis,
                                              ActiveRolesCacheRetentionProperties properties, MeterRegistry metrics) {
        return new ObservedDistributedCachePort(new RedisDistributedCachePort(redis, properties), metrics);
    }
}
