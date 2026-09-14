package co.edu.uco.seguridad.pdp.roles.infrastructure.adapter.primary.web;

import co.edu.uco.seguridad.AbstractSurrealDbIntegrationTest;
import co.edu.uco.seguridad.pdp.PdpApplication;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.secondary.policy.OpaFixtureServer;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.shared.security.TestJwtSupport;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

/**
 * Flujo HTTP completo sobre Netty, autenticado y contra una SurrealDB real — mismo patrón que
 * AuthorizationHttpTests (HU-002/HU-003).
 *
 * <p>HU-016: definir un rol {@code APPLICATION} y conceder un recurso ahora pasan por el gate de
 * administración, que consulta a OPA. Mismo {@link OpaFixtureServer} embebido que
 * {@code ApplicationHttpTests} ya usa para HU-015, respondiendo {@code ALLOW} — esta clase no prueba
 * el rechazo del gate (eso vive en {@code AdministerRoleDefinitionUseCaseImplTests}/
 * {@code AdministerResourceGrantUseCaseImplTests}), solo necesita que las fixtures funcionen.</p>
 */
@SpringBootTest(classes = PdpApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RoleHttpTests extends AbstractSurrealDbIntegrationTest {

    private static final String UCO = "universidad-uco";
    private static final String OTRO = "tenant-a";
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String OPA_ALLOW_BODY =
            "{\"result\":{\"effect\":\"ALLOW\",\"reasonCode\":\"POLICY_ALLOWED\",\"policyReferences\":[],\"obligations\":[]}}";

    private static OpaFixtureServer opa;

    @LocalServerPort
    int port;

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
        prefix = "hu004-" + UUID.randomUUID().toString().substring(0, 8);
        // HU-015: SubjectUserIdLookupValidator necesita una identidad vinculada para el subject del
        // JWT de prueba — registerApplication() pasa por el registro gateado.
        linkTestIdentity(new TenantId(UCO), "test-subject");
    }

    @Test
    void defines_a_tenant_scoped_role() {
        client().post().uri("/api/v1/roles")
                .header("Authorization", "Bearer " + TestJwtSupport.signedToken(UCO, "test-subject"))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"name":"%s-docente","scope":"TENANT"}""".formatted(prefix))
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.data.scope").isEqualTo("TENANT")
                .jsonPath("$.data.tenantId").isEqualTo(UCO)
                .jsonPath("$.data.applicationId").doesNotExist();
    }

    @Test
    void refuses_a_global_scope_on_this_channel() {
        client().post().uri("/api/v1/roles")
                .header("Authorization", "Bearer " + TestJwtSupport.signedToken(UCO, "test-subject"))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"name":"%s-super-admin","scope":"GLOBAL"}""".formatted(prefix))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo("MALFORMED_REQUEST_FIELD")
                .jsonPath("$.field").isEqualTo("scope");
    }

    @Test
    void refuses_granting_a_resource_from_another_application_to_an_application_scoped_role() {
        String ownApplicationId = registerApplication(UCO, prefix + "-app-propia");
        String otherApplicationId = registerApplication(UCO, prefix + "-app-ajena");
        registerResource(UCO, otherApplicationId, "/otros-datos", "GET");
        String foreignResourceId = findResourceId(UCO, otherApplicationId, "/otros-datos", "GET");

        String roleId = defineApplicationScopedRole(UCO, ownApplicationId, prefix + "-rol-app");

        client().post().uri("/api/v1/roles/" + roleId + "/resources")
                .header("Authorization", "Bearer " + TestJwtSupport.signedToken(UCO, "test-subject"))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"resourceId":"%s"}""".formatted(foreignResourceId))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo("RESOURCE_OUTSIDE_ROLE_SCOPE");
    }

    @Test
    void the_catalog_never_shows_roles_of_another_tenant() {
        defineTenantScopedRole(UCO, prefix + "-propio");
        defineTenantScopedRole(OTRO, prefix + "-ajeno");

        client().get().uri("/api/v1/roles")
                .header("Authorization", "Bearer " + TestJwtSupport.signedToken(UCO, "test-subject"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.content[?(@.name == '" + prefix + "-ajeno')]").doesNotExist();
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

    private void registerResource(String tenant, String applicationId, String path, String method) {
        client().post().uri("/api/v1/applications/" + applicationId + "/resources")
                .header("Authorization", "Bearer " + TestJwtSupport.signedToken(tenant, "test-subject"))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"path":"%s","method":"%s"}""".formatted(path, method))
                .exchange()
                .expectStatus().isCreated();
    }

    private String findResourceId(String tenant, String applicationId, String path, String method) {
        byte[] body = client().get().uri("/api/v1/applications/" + applicationId + "/resources")
                .header("Authorization", "Bearer " + TestJwtSupport.signedToken(tenant, "test-subject"))
                .exchange()
                .expectStatus().isOk()
                .expectBody().returnResult().getResponseBody();
        for (JsonNode resource : JSON.readTree(body).path("data")) {
            if (resource.path("path").asString().equals(path) && resource.path("method").asString().equals(method)) {
                return resource.path("id").asString();
            }
        }
        throw new IllegalStateException("recurso de prueba no encontrado: " + path + " " + method);
    }

    private WebTestClient client() {
        return WebTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
    }
}
