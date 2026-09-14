package co.edu.uco.seguridad;

import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.commons.model.UserId;
import co.edu.uco.seguridad.pdp.identity.domain.SecurityUser;
import co.edu.uco.seguridad.pdp.identity.domain.model.Email;
import co.edu.uco.seguridad.pdp.identity.domain.model.ExternalIdentity;
import co.edu.uco.seguridad.pdp.identity.infrastructure.adapter.secondary.persistence.repository.SurrealSecurityUserRepository;
import co.edu.uco.seguridad.shared.persistence.surrealdb.SurrealDbClient;
import co.edu.uco.seguridad.shared.persistence.surrealdb.SurrealDbProperties;
import co.edu.uco.seguridad.shared.security.TestJwtSupport;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.UUID;

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

    /**
     * Vincula una identidad externa para que {@code SubjectUserIdLookupValidator} (HU-015) pueda
     * resolver el {@code UserId} de un JWT crudo de prueba — el único principal que produce
     * {@link TestJwtSupport}, que nunca trae {@code userId} ya resuelto (a diferencia de
     * {@code LocalUserPrincipal}, que solo construye un login real vía Keycloak). Sin Spring:
     * construye su propio {@link SurrealDbClient} contra el mismo contenedor, igual que
     * {@code SurrealRepositoryIntegrationTests} — el esquema ya existe porque el contexto de Spring
     * de la prueba que llama esto ya arrancó y lo definió. Idempotente por {@code (issuer, subject)}
     * — hay un índice único sobre esa pareja, así que si ya existe una identidad la reutiliza en vez
     * de intentar crear otra (varias pruebas de la misma clase, o de clases distintas, llaman esto
     * con el mismo {@code subject} literal, p. ej. {@code "test-subject"}). Llamar con un
     * {@code subject} único por prueba/clase si el aislamiento importa; la base no se limpia entre
     * corridas.
     */
    protected static UserId linkTestIdentity(TenantId tenant, String subject) {
        SurrealDbProperties properties = new SurrealDbProperties(
                "http://%s:%d".formatted(SURREALDB.getHost(), SURREALDB.getMappedPort(8000)),
                "pdp", "pdp", "root", "root");
        WebClient webClient = WebClient.builder()
                .baseUrl(properties.url())
                .defaultHeaders(headers -> {
                    headers.setBasicAuth(properties.username(), properties.password());
                    headers.set("surreal-ns", properties.namespace());
                    headers.set("surreal-db", properties.database());
                })
                .build();
        SurrealDbClient client = new SurrealDbClient(webClient, new ObjectMapper(), properties);
        SurrealSecurityUserRepository repository = new SurrealSecurityUserRepository(client);
        return repository.findIdentityBySubject(subject)
                .map(ExternalIdentity::userId)
                .switchIfEmpty(Mono.defer(() -> {
                    UserId userId = new UserId(UUID.randomUUID());
                    String localPart = subject.replaceAll("[^a-zA-Z0-9]", "") + "-" + userId.value();
                    return repository.save(SecurityUser.provision(userId, tenant,
                                    new Email(localPart + "@test.local"), "Test User", Instant.now()))
                            .then(repository.linkIdentity(new ExternalIdentity(userId, TestJwtSupport.ISSUER, subject, "test")))
                            .thenReturn(userId);
                }))
                .block();
    }
}
