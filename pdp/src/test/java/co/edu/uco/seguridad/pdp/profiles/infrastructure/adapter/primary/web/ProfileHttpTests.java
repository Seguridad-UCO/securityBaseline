package co.edu.uco.seguridad.pdp.profiles.infrastructure.adapter.primary.web;

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

import java.util.UUID;

/**
 * Flujo HTTP completo sobre Netty, autenticado y contra una SurrealDB real — mismo patrón que
 * RoleHttpTests (HU-004).
 *
 * <p>HU-036: definir un perfil {@code TENANT} exige administrar al menos una aplicación del inquilino, con
 * evidencia de MFA. Mismo {@link OpaFixtureServer} y mismas propiedades de MFA que {@code RoleHttpTests},
 * y como {@code POST /api/v1/applications} deja al sujeto como primer administrador (HU-015), basta
 * registrar una aplicación antes. Esta clase no prueba el rechazo del gate (eso vive en
 * {@code AdministerProfileDefinitionUseCaseImplTests}).</p>
 */
@SpringBootTest(classes = PdpApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProfileHttpTests extends AbstractSurrealDbIntegrationTest {

    private static final String UCO = "universidad-uco";
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

    @DynamicPropertySource
    static void mfaEvidenceProperties(DynamicPropertyRegistry registry) {
        registry.add("pdp.security.mfa.claim", () -> "acr");
        registry.add("pdp.security.mfa.accepted-values", () -> TestJwtSupport.MFA_ACCEPTED_ACR);
    }

    @BeforeEach
    void freshPrefix() {
        prefix = "hu011-" + UUID.randomUUID().toString().substring(0, 8);
        // SubjectUserIdLookupValidator necesita una identidad vinculada para el subject del JWT de prueba
        linkTestIdentity(new TenantId(UCO), "test-subject");
    }

    @Test
    void defines_a_tenant_scoped_profile() {
        registerApplication(UCO, prefix + "-app-admin");

        client().post().uri("/api/v1/profiles")
                .header("Authorization", "Bearer " + TestJwtSupport.signedTokenWithMfaEvidence(UCO, "test-subject"))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"name":"%s-coordinador","scope":"TENANT"}""".formatted(prefix))
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.data.scope").isEqualTo("TENANT")
                .jsonPath("$.data.tenantId").isEqualTo(UCO)
                .jsonPath("$.data.applicationId").doesNotExist();
    }

    @Test
    void refuses_a_global_scope_on_this_channel() {
        client().post().uri("/api/v1/profiles")
                .header("Authorization", "Bearer " + TestJwtSupport.signedToken(UCO, "test-subject"))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"name":"%s-super-perfil","scope":"GLOBAL"}""".formatted(prefix))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo("MALFORMED_REQUEST_FIELD")
                .jsonPath("$.field").isEqualTo("scope");
    }

    private void registerApplication(String tenant, String name) {
        client().post().uri("/api/v1/applications")
                .header("Authorization", "Bearer " + TestJwtSupport.signedToken(tenant, "test-subject"))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"name":"%s","description":"","baseUrl":"https://example.com"}""".formatted(name))
                .exchange()
                .expectStatus().isCreated();
    }

    private WebTestClient client() {
        return WebTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
    }
}
