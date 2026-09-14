package co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.primary.web;

import co.edu.uco.seguridad.AbstractSurrealDbIntegrationTest;
import co.edu.uco.seguridad.pdp.PdpApplication;
import co.edu.uco.seguridad.pdp.assignments.application.primaryport.request.ResolveActiveRolesRequest;
import co.edu.uco.seguridad.pdp.assignments.application.usecase.ResolveActiveRolesUseCase;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.secondary.policy.OpaFixtureServer;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.RoleId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.commons.model.UserId;
import co.edu.uco.seguridad.pdp.identity.application.secondaryport.repository.SecurityUserRepository;
import co.edu.uco.seguridad.pdp.identity.domain.SecurityUser;
import co.edu.uco.seguridad.pdp.identity.domain.model.Email;
import co.edu.uco.seguridad.shared.security.TestJwtSupport;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.test.StepVerifier;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Flujo HTTP completo sobre Netty, autenticado y contra una SurrealDB real — mismo patrón que
 * RoleHttpTests (HU-004). No hay endpoint para crear usuarios (nacen del login OIDC): se siembran
 * directamente contra {@code SecurityUserRepository}, igual que un test de persistencia siembra filas.
 *
 * <p>HU-016: {@code defineApplicationScopedRole} ahora pasa por el gate de administración (¿el
 * registrador administra la aplicación?), que consulta a OPA. Mismo {@link OpaFixtureServer}
 * embebido que {@code ApplicationHttpTests} ya usa para HU-015, respondiendo {@code ALLOW} — esta
 * clase no prueba el rechazo del gate (eso vive en {@code AdministerRoleDefinitionUseCaseImplTests}),
 * solo necesita que la fixture de "aplicación con su primer administrador" funcione.</p>
 */
@SpringBootTest(classes = PdpApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AssignmentHttpTests extends AbstractSurrealDbIntegrationTest {

    private static final String UCO = "universidad-uco";
    private static final String OTRO = "tenant-a";
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String OPA_ALLOW_BODY =
            "{\"result\":{\"effect\":\"ALLOW\",\"reasonCode\":\"POLICY_ALLOWED\",\"policyReferences\":[],\"obligations\":[]}}";

    private static OpaFixtureServer opa;

    @LocalServerPort
    int port;

    @Autowired
    SecurityUserRepository userRepository;

    @Autowired
    ResolveActiveRolesUseCase resolveActiveRoles;

    private String prefix;

    @BeforeAll
    static void startOpaFixture() throws Exception {
        opa = OpaFixtureServer.start();
        opa.respondWith(200, OPA_ALLOW_BODY);
    }

    @AfterAll
    static void stopOpaFixture() {
        opa.stop();
    }

    @DynamicPropertySource
    static void opaConnectionProperties(DynamicPropertyRegistry registry) {
        registry.add("pdp.opa.base-url", () -> opa.baseUrl());
    }

    @BeforeEach
    void freshPrefix() {
        prefix = "hu005-" + UUID.randomUUID().toString().substring(0, 8);
        // HU-015: SubjectUserIdLookupValidator necesita una identidad vinculada para el subject del
        // JWT de prueba, aunque esta clase no ejerza los endpoints gateados directamente.
        linkTestIdentity(new TenantId(UCO), "test-subject");
    }

    @Test
    void assigns_a_tenant_scoped_role_to_an_application_of_the_tenant() {
        String applicationId = registerApplication(UCO, prefix + "-app");
        String roleId = defineTenantScopedRole(UCO, prefix + "-rol");
        UserId userId = seedUser(UCO, prefix + "-user1@uco.edu.co");

        assignRole(UCO, roleId, userId, applicationId)
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.data.roleId").isEqualTo(roleId)
                .jsonPath("$.data.validUntil").doesNotExist();
    }

    @Test
    void refuses_assigning_an_application_scoped_role_to_another_application() {
        String ownApplicationId = registerApplication(UCO, prefix + "-app-propia");
        String otherApplicationId = registerApplication(UCO, prefix + "-app-ajena");
        String roleId = defineApplicationScopedRole(UCO, ownApplicationId, prefix + "-rol-app");
        UserId userId = seedUser(UCO, prefix + "-user2@uco.edu.co");

        assignRole(UCO, roleId, userId, otherApplicationId)
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo("APPLICATION_OUTSIDE_ROLE_SCOPE");
    }

    @Test
    void refuses_assigning_the_same_triple_twice() {
        String applicationId = registerApplication(UCO, prefix + "-app-dup");
        String roleId = defineTenantScopedRole(UCO, prefix + "-rol-dup");
        UserId userId = seedUser(UCO, prefix + "-user3@uco.edu.co");

        assignRole(UCO, roleId, userId, applicationId).expectStatus().isCreated();

        assignRole(UCO, roleId, userId, applicationId)
                .expectStatus().isEqualTo(HttpStatus.CONFLICT)
                .expectBody()
                .jsonPath("$.code").isEqualTo("ASSIGNMENT_ALREADY_ACTIVE");
    }

    @Test
    void revoking_removes_the_role_from_the_active_context() {
        String applicationId = registerApplication(UCO, prefix + "-app-revoke");
        String roleId = defineTenantScopedRole(UCO, prefix + "-rol-revoke");
        UserId userId = seedUser(UCO, prefix + "-user4@uco.edu.co");

        byte[] body = assignRole(UCO, roleId, userId, applicationId)
                .expectStatus().isCreated()
                .expectBody().returnResult().getResponseBody();
        String assignmentId = JSON.readTree(body).path("data").path("id").asString();

        client().delete().uri("/api/v1/roles/" + roleId + "/assignments/" + assignmentId)
                .header("Authorization", "Bearer " + TestJwtSupport.signedToken(UCO, "test-subject"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo("ASSIGNMENT_REVOKED");

        StepVerifier.create(resolveActiveRoles.execute(
                        new ResolveActiveRolesRequest(userId, ApplicationId.of(applicationId))))
                .assertNext(response -> assertThat(response.roleIds()).doesNotContain(RoleId.of(roleId)))
                .verifyComplete();
    }

    @Test
    void the_catalog_never_shows_assignments_of_another_tenant() {
        String applicationId = registerApplication(UCO, prefix + "-app-list");
        String roleId = defineTenantScopedRole(UCO, prefix + "-rol-list");
        UserId ownUser = seedUser(UCO, prefix + "-user5@uco.edu.co");
        assignRole(UCO, roleId, ownUser, applicationId).expectStatus().isCreated();

        client().get().uri("/api/v1/roles/" + roleId + "/assignments")
                .header("Authorization", "Bearer " + TestJwtSupport.signedToken(OTRO, "test-subject"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.content").isEmpty();
    }

    private WebTestClient.ResponseSpec assignRole(String tenant, String roleId, UserId userId, String applicationId) {
        return client().post().uri("/api/v1/roles/" + roleId + "/assignments")
                .header("Authorization", "Bearer " + TestJwtSupport.signedToken(tenant, "test-subject"))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"userId":"%s","applicationId":"%s"}""".formatted(userId.value(), applicationId))
                .exchange();
    }

    private UserId seedUser(String tenant, String email) {
        UserId id = new UserId(UUID.randomUUID());
        SecurityUser user = SecurityUser.provision(id, new TenantId(tenant), new Email(email), "Test User", Instant.now());
        userRepository.save(user).block();
        return id;
    }

    private String defineTenantScopedRole(String tenant, String name) {
        byte[] body = client().post().uri("/api/v1/roles")
                .header("Authorization", "Bearer " + TestJwtSupport.signedToken(tenant, "test-subject"))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"name":"%s","scope":"TENANT"}""".formatted(name))
                .exchange()
                .expectStatus().isCreated()
                .expectBody().returnResult().getResponseBody();
        return JSON.readTree(body).path("data").path("id").asString();
    }

    private String defineApplicationScopedRole(String tenant, String applicationId, String name) {
        byte[] body = client().post().uri("/api/v1/roles")
                .header("Authorization", "Bearer " + TestJwtSupport.signedToken(tenant, "test-subject"))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"name":"%s","scope":"APPLICATION","applicationId":"%s"}""".formatted(name, applicationId))
                .exchange()
                .expectStatus().isCreated()
                .expectBody().returnResult().getResponseBody();
        return JSON.readTree(body).path("data").path("id").asString();
    }

    private String registerApplication(String tenant, String name) {
        byte[] body = client().post().uri("/api/v1/applications")
                .header("Authorization", "Bearer " + TestJwtSupport.signedToken(tenant, "test-subject"))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"name":"%s","description":"","baseUrl":"https://example.com"}""".formatted(name))
                .exchange()
                .expectStatus().isCreated()
                .expectBody().returnResult().getResponseBody();
        JsonNode root = JSON.readTree(body);
        return root.path("data").path("id").asString();
    }

    private WebTestClient client() {
        return WebTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
    }
}
