package co.edu.uco.seguridad;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Base para cualquier prueba que levante el contexto completo de Spring (ADR-0004): desde que
 * {@code TenantsConfiguration}/{@code ApplicationsConfiguration}/{@code ResourcesConfiguration}
 * registran un {@code ApplicationRunner} que define el esquema en el arranque, el contexto ya no
 * levanta sin una SurrealDB real detrás.
 *
 * <p>Un único contenedor, compartido por toda la JVM de prueba (arrancado en la inicialización
 * estática, nunca detenido explícitamente — Testcontainers lo destruye vía Ryuk al final de la
 * ejecución): levantar una instancia por clase de prueba haría la suite lenta sin ganar aislamiento
 * real, porque cada prueba ya usa su propio nombre de aplicación/tenant/recurso.</p>
 */
@Testcontainers
public abstract class AbstractSurrealDbIntegrationTest {

    protected static final GenericContainer<?> SURREALDB = new GenericContainer<>(DockerImageName.parse("surrealdb/surrealdb:latest"))
            .withCommand("start", "--user", "root", "--pass", "root", "memory")
            .withExposedPorts(8000)
            .waitingFor(Wait.forHttp("/health").forStatusCode(200));

    static {
        SURREALDB.start();
    }

    @DynamicPropertySource
    static void surrealDbConnectionProperties(DynamicPropertyRegistry registry) {
        registry.add("pdp.persistence.surrealdb.url",
                () -> "http://%s:%d".formatted(SURREALDB.getHost(), SURREALDB.getMappedPort(8000)));
    }
}
