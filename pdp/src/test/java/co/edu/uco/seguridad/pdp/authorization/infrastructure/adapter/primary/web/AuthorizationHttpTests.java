package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web;

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
 * Flujo HTTP completo sobre Netty, autenticado y contra una SurrealDB real. {@code pdp.opa.base-url}
 * apunta a un {@link OpaFixtureServer} embebido (HU-006) que responde DENY/NO_APPLICABLE_POLICY por
 * defecto — el mismo resultado que daba el adaptador que denegaba por defecto (ADR-012), ahora
 * viniendo de una respuesta HTTP real de "OPA" en vez de un valor fijo en el PDP. ALLOW e
 * INDETERMINATE-por-fallo-tecnico se prueban en AuthorizeUseCaseImplTests y en
 * OpaPolicyDecisionAdapterTests, que no dependen de este flujo HTTP completo.
 */
@SpringBootTest(classes = PdpApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthorizationHttpTests extends AbstractSurrealDbIntegrationTest {

    private static final String UCO = "universidad-uco";
    private static final String OTRO = "tenant-a";
    private static final ObjectMapper JSON = new ObjectMapper();

    private static OpaFixtureServer opa;

    @LocalServerPort
    int port;

    private String prefix;
    private String applicationId;

    @BeforeAll
    static void startOpaFixture() throws Exception {
        opa = OpaFixtureServer.start();
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
    void registerApplicationAndResource() {
        prefix = "hu002-" + UUID.randomUUID().toString().substring(0, 8);
        // HU-015: SubjectUserIdLookupValidator necesita una identidad vinculada para el subject del
        // JWT de prueba — registerApplication() pasa por el registro gateado.
        linkTestIdentity(new TenantId(UCO), "test-subject");
        applicationId = registerApplication(UCO, prefix + "-gestion-academica");
        registerResource(UCO, applicationId, "/estudiantes", "GET");
    }

    @Test
    void denies_by_default_when_application_and_resource_are_known() {
        authorize(UCO, applicationId, "/estudiantes", "GET")
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.state").isEqualTo("DENY")
                .jsonPath("$.data.reasonCode").isEqualTo("NO_APPLICABLE_POLICY")
                .jsonPath("$.data.decisionId").isNotEmpty()
                .jsonPath("$.data.correlationId").isNotEmpty()
                .jsonPath("$.data.policyReferences.length()").isEqualTo(0);
    }

    @Test
    void reports_tenant_mismatch_for_an_application_that_does_not_belong_to_the_tenant() {
        authorize(OTRO, applicationId, "/estudiantes", "GET")
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.state").isEqualTo("DENY")
                .jsonPath("$.data.reasonCode").isEqualTo("TENANT_MISMATCH");
    }

    @Test
    void reports_no_applicable_policy_for_an_unregistered_resource() {
        authorize(UCO, applicationId, "/no-registrado", "GET")
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.state").isEqualTo("DENY")
                .jsonPath("$.data.reasonCode").isEqualTo("NO_APPLICABLE_POLICY");
    }

    @Test
    void refuses_a_missing_required_field() {
        client().post().uri("/api/v1/authorize")
                .header("Authorization", "Bearer " + TestJwtSupport.signedToken(UCO, "test-subject"))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"resourcePath":"/estudiantes","action":"GET"}""")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo("MISSING_REQUEST_FIELD")
                .jsonPath("$.field").isEqualTo("applicationId");
    }

    @Test
    void refuses_an_unsupported_action() {
        authorize(UCO, applicationId, "/estudiantes", "TRACE")
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo("MALFORMED_REQUEST_FIELD");
    }

    @Test
    void propagates_the_incoming_correlation_id_into_the_decision() {
        client().post().uri("/api/v1/authorize")
                .header("Authorization", "Bearer " + TestJwtSupport.signedToken(UCO, "test-subject"))
                .header("X-Correlation-Id", "fixed-correlation-1")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"applicationId":"%s","resourcePath":"/estudiantes","action":"GET"}""".formatted(applicationId))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.correlationId").isEqualTo("fixed-correlation-1")
                .jsonPath("$.correlationId").isEqualTo("fixed-correlation-1");
    }

    private WebTestClient.ResponseSpec authorize(String tenant, String appId, String path, String action) {
        return client().post().uri("/api/v1/authorize")
                .header("Authorization", "Bearer " + TestJwtSupport.signedToken(tenant, "test-subject"))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"applicationId":"%s","resourcePath":"%s","action":"%s"}""".formatted(appId, path, action))
                .exchange();
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

    private void registerResource(String tenant, String appId, String path, String method) {
        client().post().uri("/api/v1/applications/" + appId + "/resources")
                .header("Authorization", "Bearer " + TestJwtSupport.signedToken(tenant, "test-subject"))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"path":"%s","method":"%s"}""".formatted(path, method))
                .exchange()
                .expectStatus().isCreated();
    }

    private WebTestClient client() {
        return WebTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
    }
}
