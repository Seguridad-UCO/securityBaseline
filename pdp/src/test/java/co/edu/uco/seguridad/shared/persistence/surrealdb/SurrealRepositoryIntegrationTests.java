package co.edu.uco.seguridad.shared.persistence.surrealdb;

import co.edu.uco.seguridad.AbstractSurrealDbIntegrationTest;
import co.edu.uco.seguridad.pdp.applications.application.secondaryport.repository.ApplicationRepository;
import co.edu.uco.seguridad.pdp.applications.domain.Application;
import co.edu.uco.seguridad.pdp.applications.domain.model.ApplicationBaseUrl;
import co.edu.uco.seguridad.pdp.applications.infrastructure.adapter.secondary.persistence.repository.SurrealApplicationRepository;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationName;
import co.edu.uco.seguridad.pdp.commons.model.ResourceId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.identity.application.secondaryport.repository.SecurityUserRepository;
import co.edu.uco.seguridad.pdp.identity.domain.model.Email;
import co.edu.uco.seguridad.pdp.identity.domain.model.ExternalIdentity;
import co.edu.uco.seguridad.pdp.identity.domain.SecurityUser;
import co.edu.uco.seguridad.pdp.identity.domain.model.UserId;
import co.edu.uco.seguridad.pdp.identity.infrastructure.adapter.secondary.persistence.repository.SurrealSecurityUserRepository;
import co.edu.uco.seguridad.pdp.resources.application.secondaryport.repository.ProtectedResourceRepository;
import co.edu.uco.seguridad.pdp.resources.domain.model.HttpVerb;
import co.edu.uco.seguridad.pdp.resources.domain.ProtectedResource;
import co.edu.uco.seguridad.pdp.resources.domain.model.ResourcePath;
import co.edu.uco.seguridad.pdp.resources.infrastructure.adapter.secondary.persistence.repository.SurrealProtectedResourceRepository;
import co.edu.uco.seguridad.pdp.tenants.application.secondaryport.repository.TenantRepository;
import co.edu.uco.seguridad.pdp.tenants.domain.Tenant;
import co.edu.uco.seguridad.pdp.tenants.domain.model.TenantName;
import co.edu.uco.seguridad.pdp.tenants.domain.model.TenantStatus;
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
                .then(client.execute("UPSERT type::record('tenant', $id) SET name = $name, status = $status;",
                        Map.of("id", "surreal-it-tenant", "name", "Surreal IT Tenant", "status", "ACTIVE")))
                .block();

        TenantRepository repository = new SurrealTenantRepository(client);

        StepVerifier.create(repository.findStatusById(new TenantId("surreal-it-tenant")))
                .expectNext(TenantStatus.ACTIVE)
                .verifyComplete();

        StepVerifier.create(repository.findStatusById(new TenantId("no-such-tenant")))
                .verifyComplete();
    }

    @Test
    void tenant_repository_saves_a_new_tenant_and_reports_its_existence() {
        client.ensureNamespaceAndDatabase()
                .then(client.execute("DEFINE TABLE IF NOT EXISTS tenant SCHEMALESS;", Map.of()))
                .block();

        TenantRepository repository = new SurrealTenantRepository(client);
        TenantId id = new TenantId("surreal-it-created-tenant");

        StepVerifier.create(repository.existsById(id)).expectNext(false).verifyComplete();

        Tenant tenant = Tenant.register(id, new TenantName("Created Tenant"));
        StepVerifier.create(repository.save(tenant)).expectNext(tenant).verifyComplete();

        StepVerifier.create(repository.existsById(id)).expectNext(true).verifyComplete();

        StepVerifier.create(repository.findAll().collectList())
                .assertNext(found -> assertThat(found).extracting(Tenant::id).contains(id))
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
        Application application = Application.register(new ApplicationId(UUID.randomUUID()), tenant, name,
                "Aplicación de prueba", new ApplicationBaseUrl("https://surreal-it-app.example.com"), Instant.now());

        StepVerifier.create(repository.existsByTenantAndName(tenant, name))
                .expectNext(false)
                .verifyComplete();

        StepVerifier.create(repository.save(application)).expectNext(application).verifyComplete();

        StepVerifier.create(repository.existsByTenantAndName(tenant, name))
                .expectNext(true)
                .verifyComplete();

        StepVerifier.create(repository.existsByTenantAndId(tenant, application.id()))
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
                        DEFINE INDEX IF NOT EXISTS protected_resource_endpoint ON protected_resource \
                        COLUMNS applicationId, path, method UNIQUE;\
                        """,
                        Map.of()))
                .block();

        ProtectedResourceRepository repository = new SurrealProtectedResourceRepository(client);
        TenantId tenant = new TenantId("surreal-it-resources");
        ApplicationId applicationId = new ApplicationId(UUID.randomUUID());
        ResourcePath path = new ResourcePath("/estudiantes");
        ProtectedResource resource = ProtectedResource.register(
                new ResourceId(UUID.randomUUID()), applicationId, tenant, path, HttpVerb.GET, Instant.now());

        StepVerifier.create(repository.existsByApplicationPathAndMethod(applicationId, path, HttpVerb.GET))
                .expectNext(false)
                .verifyComplete();

        StepVerifier.create(repository.save(resource)).expectNext(resource).verifyComplete();

        StepVerifier.create(repository.existsByApplicationPathAndMethod(applicationId, path, HttpVerb.GET))
                .expectNext(true)
                .verifyComplete();

        StepVerifier.create(repository.existsByApplicationPathAndMethod(
                        new ApplicationId(UUID.randomUUID()), path, HttpVerb.GET))
                .as("the same path/method must not leak across applications")
                .expectNext(false)
                .verifyComplete();

        StepVerifier.create(repository.findAllByApplication(applicationId))
                .assertNext(found -> assertThat(found).isEqualTo(resource))
                .verifyComplete();

        StepVerifier.create(repository.deleteById(resource.id())).verifyComplete();

        StepVerifier.create(repository.findAllByApplication(applicationId)).verifyComplete();
    }

    @Test
    void security_user_repository_links_an_identity_and_finds_it_by_issuer_and_subject() {
        client.ensureNamespaceAndDatabase()
                .then(client.execute(
                        """
                        DEFINE TABLE IF NOT EXISTS security_user SCHEMALESS;
                        DEFINE TABLE IF NOT EXISTS external_identity SCHEMALESS;
                        """,
                        Map.of()))
                .block();

        SecurityUserRepository repository = new SurrealSecurityUserRepository(client);
        SecurityUser user = SecurityUser.provision(new UserId(UUID.randomUUID()),
                new TenantId("surreal-it-identity"), new Email("surreal-it-user@uco.edu"), "Surreal IT User",
                Instant.now());

        StepVerifier.create(repository.save(user)).expectNext(user).verifyComplete();

        ExternalIdentity identity = new ExternalIdentity(user.id(), "surreal-it-issuer", "surreal-it-subject", "google");
        StepVerifier.create(repository.linkIdentity(identity)).verifyComplete();

        StepVerifier.create(repository.findIdentity("surreal-it-issuer", "surreal-it-subject"))
                .assertNext(found -> assertThat(found.userId()).isEqualTo(user.id()))
                .verifyComplete();

        StepVerifier.create(repository.findByEmail(user.email()))
                .assertNext(found -> assertThat(found.id()).isEqualTo(user.id()))
                .verifyComplete();

        StepVerifier.create(repository.findById(user.id()))
                .assertNext(found -> assertThat(found).isEqualTo(user))
                .verifyComplete();

        StepVerifier.create(repository.providerFor(user.id())).expectNext("google").verifyComplete();

        StepVerifier.create(repository.findAll().collectList())
                .assertNext(found -> assertThat(found).extracting(SecurityUser::id).contains(user.id()))
                .verifyComplete();
    }
}
