package co.edu.uco.seguridad.shared.persistence.surrealdb;

import co.edu.uco.seguridad.AbstractSurrealDbIntegrationTest;
import co.edu.uco.seguridad.pdp.applications.application.port.secondary.repository.ApplicationRepository;
import co.edu.uco.seguridad.pdp.applications.domain.Application;
import co.edu.uco.seguridad.pdp.applications.infrastructure.adapter.secondary.persistence.repository.SurrealApplicationRepository;
import co.edu.uco.seguridad.pdp.commons.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.ApplicationName;
import co.edu.uco.seguridad.pdp.commons.PageWindow;
import co.edu.uco.seguridad.pdp.commons.ResourceId;
import co.edu.uco.seguridad.pdp.commons.TenantId;
import co.edu.uco.seguridad.pdp.resources.application.port.secondary.repository.ProtectedResourceRepository;
import co.edu.uco.seguridad.pdp.resources.domain.ActionCode;
import co.edu.uco.seguridad.pdp.resources.domain.ProtectedApplicationCriteria;
import co.edu.uco.seguridad.pdp.resources.domain.ProtectedResource;
import co.edu.uco.seguridad.pdp.resources.domain.ResourceCode;
import co.edu.uco.seguridad.pdp.resources.infrastructure.adapter.secondary.persistence.repository.SurrealProtectedResourceRepository;
import co.edu.uco.seguridad.pdp.tenants.application.port.secondary.repository.TenantRepository;
import co.edu.uco.seguridad.pdp.tenants.domain.TenantStatus;
import co.edu.uco.seguridad.pdp.tenants.infrastructure.adapter.secondary.persistence.repository.SurrealTenantRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Los tres adaptadores reales (ADR-0004) contra una SurrealDB real, sin levantar el contexto de
 * Spring — cada uno se construye a mano con el mismo contenedor compartido que usan las pruebas
 * HTTP, apuntando a filas con nombres únicos por prueba para no depender del orden de ejecución.
 */
class SurrealRepositoryIntegrationTests extends AbstractSurrealDbIntegrationTest {

    private static SurrealDbClient client;

    @BeforeAll
    static void setUpClient() {
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
        client = new SurrealDbClient(webClient, new ObjectMapper(), properties);
    }

    @Test
    void tenant_repository_finds_a_seeded_tenant_and_returns_empty_for_an_unknown_one() {
        client.ensureNamespaceAndDatabase()
                .then(client.execute("DEFINE TABLE IF NOT EXISTS tenant SCHEMALESS;", Map.of()))
                .then(client.execute("UPSERT type::record('tenant', $id) SET status = $status;",
                        Map.of("id", "surreal-it-tenant", "status", "ACTIVE")))
                .block();

        TenantRepository repository = new SurrealTenantRepository(client);

        StepVerifier.create(repository.findById(new TenantId("surreal-it-tenant")))
                .assertNext(tenant -> {
                    assertThat(tenant.id()).isEqualTo(new TenantId("surreal-it-tenant"));
                    assertThat(tenant.status()).isEqualTo(TenantStatus.ACTIVE);
                })
                .verifyComplete();

        StepVerifier.create(repository.findById(new TenantId("no-such-tenant")))
                .verifyComplete();
    }

    @Test
    void application_repository_detects_existence_only_after_saving() {
        client.ensureNamespaceAndDatabase()
                .then(client.execute(
                        """
                        DEFINE TABLE IF NOT EXISTS application SCHEMALESS;
                        DEFINE INDEX IF NOT EXISTS application_tenant_name ON application \
                        COLUMNS tenantId, name UNIQUE;\
                        """,
                        Map.of()))
                .block();

        ApplicationRepository repository = new SurrealApplicationRepository(client);
        TenantId tenant = new TenantId("surreal-it-apps");
        ApplicationName name = new ApplicationName("surreal-it-app");
        Application application = Application.register(
                new ApplicationId(UUID.randomUUID()), tenant, name, Instant.now());

        StepVerifier.create(repository.existsByTenantAndName(tenant, name))
                .expectNext(false)
                .verifyComplete();

        StepVerifier.create(repository.save(application)).expectNext(application).verifyComplete();

        StepVerifier.create(repository.existsByTenantAndName(tenant, name))
                .expectNext(true)
                .verifyComplete();

        StepVerifier.create(repository.deleteById(application.id())).verifyComplete();

        StepVerifier.create(repository.existsByTenantAndName(tenant, name))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    void protected_resource_repository_saves_finds_and_deletes_across_a_real_round_trip() {
        client.ensureNamespaceAndDatabase()
                .then(client.execute(
                        """
                        DEFINE TABLE IF NOT EXISTS protected_resource SCHEMALESS;
                        DEFINE INDEX IF NOT EXISTS protected_resource_grant ON protected_resource \
                        COLUMNS applicationId, resourceCode, action UNIQUE;\
                        """,
                        Map.of()))
                .block();

        ProtectedResourceRepository repository = new SurrealProtectedResourceRepository(client);
        TenantId tenant = new TenantId("surreal-it-resources");
        ApplicationId applicationId = new ApplicationId(UUID.randomUUID());
        ProtectedResource resource = ProtectedResource.register(
                new ResourceId(UUID.randomUUID()),
                applicationId,
                tenant,
                new ApplicationName("surreal-it-catalog"),
                new ResourceCode("estudiantes"),
                new ActionCode("consultar"),
                Instant.now());

        StepVerifier.create(repository.existsGrant(tenant, applicationId, resource.code(), resource.action()))
                .expectNext(false)
                .verifyComplete();

        StepVerifier.create(repository.save(resource)).expectNext(resource).verifyComplete();

        StepVerifier.create(repository.existsGrant(tenant, applicationId, resource.code(), resource.action()))
                .expectNext(true)
                .verifyComplete();

        StepVerifier.create(repository.existsGrant(
                        new TenantId("another-tenant"), applicationId, resource.code(), resource.action()))
                .as("the same applicationId/resourceCode/action must not leak across tenants")
                .expectNext(false)
                .verifyComplete();

        StepVerifier.create(repository.findBy(
                        ProtectedApplicationCriteria.scopedTo(tenant), PageWindow.defaultWindow()))
                .assertNext(page -> {
                    assertThat(page.total()).isEqualTo(1);
                    assertThat(page.content()).containsExactly(resource);
                })
                .verifyComplete();

        StepVerifier.create(repository.deleteById(resource.id())).verifyComplete();

        StepVerifier.create(repository.findBy(
                        ProtectedApplicationCriteria.scopedTo(tenant), PageWindow.defaultWindow()))
                .assertNext(page -> assertThat(page.total()).isZero())
                .verifyComplete();
    }
}
