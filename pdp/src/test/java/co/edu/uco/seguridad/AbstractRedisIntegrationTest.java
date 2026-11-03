package co.edu.uco.seguridad;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Base para cualquier prueba que necesite una Redis real (HU-022, ADR-026) — mismo patrón que
 * {@link AbstractSurrealDbIntegrationTest}: no hay módulo oficial de Testcontainers para Redis en
 * este proyecto, así que se usa {@link GenericContainer} directamente con la imagen
 * {@code redis:7-alpine}.
 *
 * <p>Un único contenedor, compartido por toda la JVM de prueba (arrancado en la inicialización
 * estática, nunca detenido explícitamente — Testcontainers lo destruye vía Ryuk al final de la
 * ejecución): las claves de revocación se nombran por {@code UserId} único por prueba, así que
 * compartir el contenedor no compromete el aislamiento.</p>
 */
@Testcontainers
public abstract class AbstractRedisIntegrationTest {

    /**
     * {@code public}, no {@code protected}: una clase que ya extiende {@code AbstractSurrealDbIntegrationTest}
     * (herencia simple) no puede heredar también de esta — referencia este campo directamente en su
     * propio {@code @DynamicPropertySource} (dispara la inicialización estática igual que extender la
     * clase) en vez de duplicar el contenedor. Ver {@code AssignmentHttpTests},
     * {@code InternalSecurityChainIntegrationTests}.
     */
    public static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379)
            .waitingFor(Wait.forListeningPort());

    static {
        REDIS.start();
    }

    @DynamicPropertySource
    static void redisConnectionProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    }
}
