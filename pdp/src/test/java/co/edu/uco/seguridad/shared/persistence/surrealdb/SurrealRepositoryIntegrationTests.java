package co.edu.uco.seguridad.shared.persistence.surrealdb;

import co.edu.uco.seguridad.AbstractSurrealDbIntegrationTest;
import co.edu.uco.seguridad.pdp.assignments.application.secondaryport.repository.AssignmentRepository;
import co.edu.uco.seguridad.pdp.assignments.domain.Assignment;
import co.edu.uco.seguridad.pdp.assignments.domain.AssignmentCriteria;
import co.edu.uco.seguridad.pdp.assignments.domain.model.AssignmentId;
import co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.secondary.persistence.repository.SurrealAssignmentRepository;
import co.edu.uco.seguridad.pdp.applications.application.secondaryport.repository.ApplicationRepository;
import co.edu.uco.seguridad.pdp.applications.domain.Application;
import co.edu.uco.seguridad.pdp.applications.domain.model.ApplicationBaseUrl;
import co.edu.uco.seguridad.pdp.applications.infrastructure.adapter.secondary.persistence.repository.SurrealApplicationRepository;
import co.edu.uco.seguridad.pdp.authorization.application.secondaryport.AccessAuditRepository;
import co.edu.uco.seguridad.pdp.authorization.domain.event.AccessEvent;
import co.edu.uco.seguridad.pdp.authorization.domain.model.DecisionState;
import co.edu.uco.seguridad.pdp.authorization.domain.model.ReasonCode;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.secondary.persistence.repository.SurrealAccessAuditRepository;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationName;
import co.edu.uco.seguridad.pdp.commons.model.PageWindow;
import co.edu.uco.seguridad.pdp.commons.model.ResourceId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.identity.application.secondaryport.repository.SecurityUserRepository;
import co.edu.uco.seguridad.pdp.identity.domain.model.Email;
import co.edu.uco.seguridad.pdp.identity.domain.model.ExternalIdentity;
import co.edu.uco.seguridad.pdp.identity.domain.SecurityUser;
import co.edu.uco.seguridad.pdp.commons.model.UserId;
import co.edu.uco.seguridad.pdp.identity.infrastructure.adapter.secondary.persistence.repository.SurrealSecurityUserRepository;
import co.edu.uco.seguridad.pdp.resources.application.secondaryport.repository.ProtectedResourceRepository;
import co.edu.uco.seguridad.pdp.resources.domain.model.HttpVerb;
import co.edu.uco.seguridad.pdp.resources.domain.ProtectedResource;
import co.edu.uco.seguridad.pdp.resources.domain.model.ResourcePath;
import co.edu.uco.seguridad.pdp.resources.infrastructure.adapter.secondary.persistence.repository.SurrealProtectedResourceRepository;
import co.edu.uco.seguridad.pdp.roles.application.secondaryport.repository.RoleRepository;
import co.edu.uco.seguridad.pdp.roles.domain.Role;
import co.edu.uco.seguridad.pdp.roles.domain.RoleCriteria;
import co.edu.uco.seguridad.pdp.roles.domain.model.RoleName;
import co.edu.uco.seguridad.pdp.roles.domain.model.RoleScope;
import co.edu.uco.seguridad.pdp.commons.model.RoleId;
import co.edu.uco.seguridad.pdp.roles.infrastructure.adapter.secondary.persistence.repository.SurrealRoleRepository;
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
import java.util.List;
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

        // HU-003: findTenantIdById resuelve el dueño solo con el id, sin conocer el tenant de antemano.
        StepVerifier.create(repository.findTenantIdById(application.id()))
                .expectNext(tenant)
                .verifyComplete();

        StepVerifier.create(repository.deleteById(application.id())).verifyComplete();

        StepVerifier.create(repository.existsByTenantAndName(tenant, name))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    void application_repository_finds_no_tenant_for_an_unknown_application_id() {
        ApplicationRepository repository = new SurrealApplicationRepository(client);

        StepVerifier.create(repository.findTenantIdById(new ApplicationId(UUID.randomUUID())))
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
    void protected_resource_repository_resolves_the_owner_application_and_empty_for_an_unknown_resource() {
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
        ApplicationId applicationId = new ApplicationId(UUID.randomUUID());
        ProtectedResource resource = ProtectedResource.register(new ResourceId(UUID.randomUUID()), applicationId,
                new TenantId("surreal-it-owner-lookup"), new ResourcePath("/notas"), HttpVerb.GET, Instant.now());
        StepVerifier.create(repository.save(resource)).expectNext(resource).verifyComplete();

        StepVerifier.create(repository.findApplicationIdById(resource.id()))
                .expectNext(applicationId)
                .verifyComplete();

        StepVerifier.create(repository.findApplicationIdById(new ResourceId(UUID.randomUUID())))
                .verifyComplete();
    }

    @Test
    void role_repository_saves_and_finds_a_role_with_its_granted_resources() {
        client.ensureNamespaceAndDatabase()
                .then(client.execute(
                        """
                        DEFINE TABLE IF NOT EXISTS role SCHEMALESS;
                        DEFINE INDEX IF NOT EXISTS role_scope_name ON role \
                        COLUMNS level, tenantId, applicationId, name UNIQUE;\
                        """,
                        Map.of()))
                .block();

        RoleRepository repository = new SurrealRoleRepository(client);
        TenantId tenant = new TenantId("surreal-it-roles");
        RoleName name = new RoleName("surreal-it-docente");
        Role role = Role.define(new RoleId(UUID.randomUUID()), name, RoleScope.ofTenant(tenant), Instant.now())
                .withResource(new ResourceId(UUID.randomUUID()));

        StepVerifier.create(repository.existsByNameInScope(name, RoleScope.ofTenant(tenant)))
                .expectNext(false)
                .verifyComplete();

        StepVerifier.create(repository.save(role)).expectNext(role).verifyComplete();

        StepVerifier.create(repository.existsByNameInScope(name, RoleScope.ofTenant(tenant)))
                .expectNext(true)
                .verifyComplete();

        StepVerifier.create(repository.findByIdForTenant(role.id(), tenant))
                .assertNext(found -> assertThat(found.resources()).isEqualTo(role.resources()))
                .verifyComplete();
    }

    @Test
    void role_repository_finds_no_role_for_a_tenant_it_does_not_belong_to() {
        client.ensureNamespaceAndDatabase()
                .then(client.execute("DEFINE TABLE IF NOT EXISTS role SCHEMALESS;", Map.of()))
                .block();

        RoleRepository repository = new SurrealRoleRepository(client);
        TenantId owner = new TenantId("surreal-it-role-owner");
        TenantId stranger = new TenantId("surreal-it-role-stranger");
        Role role = Role.define(new RoleId(UUID.randomUUID()), new RoleName("surreal-it-ajeno"),
                RoleScope.ofTenant(owner), Instant.now());
        StepVerifier.create(repository.save(role)).expectNext(role).verifyComplete();

        StepVerifier.create(repository.findByIdForTenant(role.id(), stranger)).verifyComplete();
    }

    @Test
    void role_repository_lists_the_tenant_catalog_including_globals_and_excluding_other_tenants() {
        client.ensureNamespaceAndDatabase()
                .then(client.execute("DEFINE TABLE IF NOT EXISTS role SCHEMALESS;", Map.of()))
                .block();

        RoleRepository repository = new SurrealRoleRepository(client);
        TenantId tenant = new TenantId("surreal-it-catalog-" + UUID.randomUUID());
        TenantId other = new TenantId("surreal-it-catalog-other-" + UUID.randomUUID());
        Role ownRole = Role.define(new RoleId(UUID.randomUUID()), new RoleName("propio"), RoleScope.ofTenant(tenant),
                Instant.now());
        Role globalRole = Role.define(new RoleId(UUID.randomUUID()), new RoleName("global-" + UUID.randomUUID()),
                RoleScope.global(), Instant.now());
        Role otherRole = Role.define(new RoleId(UUID.randomUUID()), new RoleName("ajeno"), RoleScope.ofTenant(other),
                Instant.now());
        StepVerifier.create(repository.save(ownRole)).expectNextCount(1).verifyComplete();
        StepVerifier.create(repository.save(globalRole)).expectNextCount(1).verifyComplete();
        StepVerifier.create(repository.save(otherRole)).expectNextCount(1).verifyComplete();

        StepVerifier.create(repository.findBy(RoleCriteria.ofTenant(tenant), PageWindow.defaultWindow()))
                .assertNext(page -> {
                    List<RoleId> ids = page.content().stream().map(Role::id).toList();
                    assertThat(ids).contains(ownRole.id(), globalRole.id());
                    assertThat(ids).doesNotContain(otherRole.id());
                })
                .verifyComplete();
    }

    @Test
    void role_repository_finds_a_global_role_without_filtering_by_tenant() {
        client.ensureNamespaceAndDatabase()
                .then(client.execute("DEFINE TABLE IF NOT EXISTS role SCHEMALESS;", Map.of()))
                .block();

        RoleRepository repository = new SurrealRoleRepository(client);
        Role globalRole = Role.define(new RoleId(UUID.randomUUID()), new RoleName("global-" + UUID.randomUUID()),
                RoleScope.global(), Instant.now());
        StepVerifier.create(repository.save(globalRole)).expectNextCount(1).verifyComplete();

        StepVerifier.create(repository.findById(globalRole.id()))
                .assertNext(found -> assertThat(found.scope()).isEqualTo(RoleScope.global()))
                .verifyComplete();

        StepVerifier.create(repository.findById(new RoleId(UUID.randomUUID()))).verifyComplete();
    }

    @Test
    void assignment_repository_saves_and_reports_an_active_assignment() {
        client.ensureNamespaceAndDatabase()
                .then(client.execute("DEFINE TABLE IF NOT EXISTS assignment SCHEMALESS;", Map.of()))
                .block();

        AssignmentRepository repository = new SurrealAssignmentRepository(client);
        TenantId tenant = new TenantId("surreal-it-assignments-" + UUID.randomUUID());
        UserId user = new UserId(UUID.randomUUID());
        ApplicationId application = new ApplicationId(UUID.randomUUID());
        RoleId role = new RoleId(UUID.randomUUID());
        Instant now = Instant.now();

        StepVerifier.create(repository.existsActiveByUserApplicationRole(user, application, role, now))
                .expectNext(false)
                .verifyComplete();

        Assignment assignment = Assignment.assign(new AssignmentId(UUID.randomUUID()), user, tenant, application, role, now);
        StepVerifier.create(repository.save(assignment)).expectNext(assignment).verifyComplete();

        StepVerifier.create(repository.existsActiveByUserApplicationRole(user, application, role, now))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    void assignment_repository_finds_no_assignment_for_a_tenant_it_does_not_belong_to() {
        client.ensureNamespaceAndDatabase()
                .then(client.execute("DEFINE TABLE IF NOT EXISTS assignment SCHEMALESS;", Map.of()))
                .block();

        AssignmentRepository repository = new SurrealAssignmentRepository(client);
        TenantId owner = new TenantId("surreal-it-asg-owner-" + UUID.randomUUID().toString().substring(0, 8));
        TenantId stranger = new TenantId("surreal-it-asg-stranger-" + UUID.randomUUID().toString().substring(0, 8));
        Assignment assignment = Assignment.assign(new AssignmentId(UUID.randomUUID()), new UserId(UUID.randomUUID()), owner,
                new ApplicationId(UUID.randomUUID()), new RoleId(UUID.randomUUID()), Instant.now());
        StepVerifier.create(repository.save(assignment)).expectNext(assignment).verifyComplete();

        StepVerifier.create(repository.findByIdForTenant(assignment.id(), stranger)).verifyComplete();
    }

    @Test
    void assignment_repository_lists_the_role_catalog_excluding_other_tenants() {
        client.ensureNamespaceAndDatabase()
                .then(client.execute("DEFINE TABLE IF NOT EXISTS assignment SCHEMALESS;", Map.of()))
                .block();

        AssignmentRepository repository = new SurrealAssignmentRepository(client);
        RoleId role = new RoleId(UUID.randomUUID());
        TenantId tenant = new TenantId("surreal-it-asg-list-" + UUID.randomUUID().toString().substring(0, 8));
        TenantId other = new TenantId("surreal-it-asg-list-o-" + UUID.randomUUID().toString().substring(0, 8));
        Assignment ownAssignment = Assignment.assign(new AssignmentId(UUID.randomUUID()), new UserId(UUID.randomUUID()),
                tenant, new ApplicationId(UUID.randomUUID()), role, Instant.now());
        Assignment otherAssignment = Assignment.assign(new AssignmentId(UUID.randomUUID()), new UserId(UUID.randomUUID()),
                other, new ApplicationId(UUID.randomUUID()), role, Instant.now());
        StepVerifier.create(repository.save(ownAssignment)).expectNextCount(1).verifyComplete();
        StepVerifier.create(repository.save(otherAssignment)).expectNextCount(1).verifyComplete();

        StepVerifier.create(repository.findBy(AssignmentCriteria.of(role, tenant), PageWindow.defaultWindow()))
                .assertNext(page -> {
                    List<AssignmentId> ids = page.content().stream().map(Assignment::id).toList();
                    assertThat(ids).contains(ownAssignment.id());
                    assertThat(ids).doesNotContain(otherAssignment.id());
                })
                .verifyComplete();
    }

    @Test
    void assignment_repository_excludes_a_revoked_assignment_from_active_roles() {
        client.ensureNamespaceAndDatabase()
                .then(client.execute("DEFINE TABLE IF NOT EXISTS assignment SCHEMALESS;", Map.of()))
                .block();

        AssignmentRepository repository = new SurrealAssignmentRepository(client);
        UserId user = new UserId(UUID.randomUUID());
        ApplicationId application = new ApplicationId(UUID.randomUUID());
        RoleId role = new RoleId(UUID.randomUUID());
        Instant past = Instant.now().minusSeconds(3600);
        Assignment revoked = Assignment.assign(new AssignmentId(UUID.randomUUID()), user,
                        new TenantId("surreal-it-asg-active-" + UUID.randomUUID().toString().substring(0, 8)), application,
                        role, past.minusSeconds(3600))
                .revoke(past);
        StepVerifier.create(repository.save(revoked)).expectNext(revoked).verifyComplete();

        StepVerifier.create(repository.findActiveRoleIdsFor(user, application, Instant.now()))
                .assertNext(roleIds -> assertThat(roleIds).doesNotContain(role))
                .verifyComplete();
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

    @Test
    void access_audit_repository_saves_an_event_and_finds_it_by_correlation_id() {
        client.ensureNamespaceAndDatabase()
                .then(client.execute("DEFINE TABLE IF NOT EXISTS access_event SCHEMALESS;", Map.of()))
                .block();

        AccessAuditRepository repository = new SurrealAccessAuditRepository(client);
        String correlationId = "surreal-it-audit-" + UUID.randomUUID();
        AccessEvent event = new AccessEvent(UUID.randomUUID(), UUID.randomUUID(), "req-1", correlationId,
                new TenantId("surreal-it-audit-tenant"), new ApplicationId(UUID.randomUUID()), "test-subject",
                new ResourcePath("/estudiantes"), HttpVerb.GET, DecisionState.DENY, ReasonCode.NO_APPLICABLE_POLICY,
                Instant.now());

        StepVerifier.create(repository.save(event)).verifyComplete();

        StepVerifier.create(repository.findByCorrelationId(correlationId).collectList())
                .assertNext(found -> assertThat(found).extracting(AccessEvent::eventId).containsExactly(event.eventId()))
                .verifyComplete();
    }

    @Test
    void access_audit_repository_finds_both_events_sharing_a_correlation_id() {
        client.ensureNamespaceAndDatabase()
                .then(client.execute("DEFINE TABLE IF NOT EXISTS access_event SCHEMALESS;", Map.of()))
                .block();

        AccessAuditRepository repository = new SurrealAccessAuditRepository(client);
        String correlationId = "surreal-it-audit-shared-" + UUID.randomUUID();
        TenantId tenant = new TenantId("surreal-it-audit-shared-tenant");
        ApplicationId application = new ApplicationId(UUID.randomUUID());
        AccessEvent first = new AccessEvent(UUID.randomUUID(), UUID.randomUUID(), "req-1", correlationId, tenant,
                application, "test-subject", new ResourcePath("/estudiantes"), HttpVerb.GET, DecisionState.DENY,
                ReasonCode.NO_APPLICABLE_POLICY, Instant.now());
        AccessEvent second = new AccessEvent(UUID.randomUUID(), UUID.randomUUID(), "req-2", correlationId, tenant,
                application, "test-subject", new ResourcePath("/estudiantes"), HttpVerb.GET, DecisionState.ALLOW,
                ReasonCode.POLICY_ALLOWED, Instant.now());

        StepVerifier.create(repository.save(first)).verifyComplete();
        StepVerifier.create(repository.save(second)).verifyComplete();

        StepVerifier.create(repository.findByCorrelationId(correlationId).collectList())
                .assertNext(found -> assertThat(found).extracting(AccessEvent::eventId)
                        .containsExactlyInAnyOrder(first.eventId(), second.eventId()))
                .verifyComplete();
    }
}
