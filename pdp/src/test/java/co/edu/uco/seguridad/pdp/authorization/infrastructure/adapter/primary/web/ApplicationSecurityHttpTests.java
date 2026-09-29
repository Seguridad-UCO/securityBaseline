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

/** HTTP contract for the lazy, application-scoped security read model. */
@SpringBootTest(classes = PdpApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ApplicationSecurityHttpTests extends AbstractSurrealDbIntegrationTest {
    private static final String TENANT = "universidad-uco";
    private static final String ADMIN_PATH = "/v1/data/security/administration/decision";
    private static final String ALLOW = "{\"result\":{\"effect\":\"ALLOW\",\"reasonCode\":\"POLICY_ALLOWED\",\"policyReferences\":[],\"obligations\":[]}}";
    private static final String DENY = "{\"result\":{\"effect\":\"DENY\",\"reasonCode\":\"POLICY_DENIED\",\"policyReferences\":[],\"obligations\":[]}}";
    private static final ObjectMapper JSON = new ObjectMapper();
    private static OpaFixtureServer opa;

    @LocalServerPort int port;
    private String applicationId;

    @BeforeAll static void startOpa() throws Exception { opa = OpaFixtureServer.start(); }
    @AfterAll static void stopOpa() { opa.stop(); }
    @DynamicPropertySource static void opaProperties(DynamicPropertyRegistry registry) { registry.add("pdp.opa.base-url", () -> opa.baseUrl()); }
    @DynamicPropertySource static void mfaProperties(DynamicPropertyRegistry registry) {
        registry.add("pdp.security.mfa.claim", () -> "acr");
        registry.add("pdp.security.mfa.accepted-values", () -> TestJwtSupport.MFA_ACCEPTED_ACR);
    }

    @BeforeEach void createApplication() {
        opa.respondWithForPath(ADMIN_PATH, 200, ALLOW);
        linkTestIdentity(new TenantId(TENANT), "test-subject");
        applicationId = registerApplication("security-" + UUID.randomUUID().toString().substring(0, 8));
        client().post().uri("/api/v1/applications/" + applicationId + "/resources")
                .header("Authorization", bearer()).contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"path\":\"/notes\",\"method\":\"GET\"}")
                .exchange().expectStatus().isCreated();
    }

    @Test void summary_is_small_and_all_collection_routes_use_page_response() {
        client().get().uri(security("summary")).header("Authorization", bearer()).exchange()
                .expectStatus().isOk().expectBody()
                .jsonPath("$.data.application.id").isEqualTo(applicationId)
                .jsonPath("$.data.counts.resources").isEqualTo(1)
                .jsonPath("$.data.resources").doesNotExist()
                .jsonPath("$.data.content").doesNotExist();

        for (String collection : new String[] { "resources", "roles", "profiles", "administrators", "role-assignments", "profile-assignments" }) {
            client().get().uri(security(collection) + "?page=0&size=20").header("Authorization", bearer()).exchange()
                    .expectStatus().isOk().expectBody()
                    .jsonPath("$.data.content").isArray()
                    .jsonPath("$.data.total").isNumber()
                    .jsonPath("$.data.page").isEqualTo(0)
                    .jsonPath("$.data.offset").isEqualTo(0)
                    .jsonPath("$.data.limit").isEqualTo(20);
        }
        client().get().uri(security("users") + "?query=missing&page=0&size=20").header("Authorization", bearer()).exchange()
                .expectStatus().isOk().expectBody()
                .jsonPath("$.data.content").isArray()
                .jsonPath("$.data.page").isEqualTo(0)
                .jsonPath("$.data.limit").isEqualTo(20);
    }

    @Test void supports_offset_limit_and_rejects_mixed_pagination_styles() {
        client().get().uri(security("resources") + "?offset=0&limit=1").header("Authorization", bearer()).exchange()
                .expectStatus().isOk().expectBody().jsonPath("$.data.page").isEqualTo(0).jsonPath("$.data.limit").isEqualTo(1);
        client().get().uri(security("resources") + "?page=0&offset=0").header("Authorization", bearer()).exchange()
                .expectStatus().isBadRequest();
    }

    @Test void denies_all_security_reads_when_the_administration_gate_denies() {
        opa.respondWithForPath(ADMIN_PATH, 200, DENY);
        client().get().uri(security("roles") + "?page=0&size=20").header("Authorization", bearer()).exchange()
                .expectStatus().isForbidden();
    }

    @Test void relationship_routes_are_application_scoped_and_paged() throws Exception {
        String resourceId = JSON.readTree(client().get().uri(security("resources") + "?page=0&size=20")
                .header("Authorization", bearer()).exchange().expectStatus().isOk().expectBody().returnResult().getResponseBody())
                .path("data").path("content").get(0).path("id").asString();
        String roleId = idOf(client().post().uri("/api/v1/roles").header("Authorization", bearer())
                .contentType(MediaType.APPLICATION_JSON).bodyValue("{\"name\":\"LECTOR\",\"scope\":\"APPLICATION\",\"applicationId\":\"%s\"}".formatted(applicationId))
                .exchange().expectStatus().isCreated().expectBody().returnResult().getResponseBody());
        client().post().uri("/api/v1/roles/" + roleId + "/resources").header("Authorization", bearer())
                .contentType(MediaType.APPLICATION_JSON).bodyValue("{\"resourceId\":\"%s\"}".formatted(resourceId)).exchange().expectStatus().isOk();
        client().get().uri(security("roles/" + roleId + "/resources?page=0&size=1")).header("Authorization", bearer()).exchange()
                .expectStatus().isOk().expectBody().jsonPath("$.data.content[0].id").isEqualTo(resourceId).jsonPath("$.data.total").isEqualTo(1).jsonPath("$.data.limit").isEqualTo(1);
    }

    @Test void profile_role_relationship_route_is_application_scoped_and_paged() throws Exception {
        String roleId = idOf(client().post().uri("/api/v1/roles").header("Authorization", bearer()).contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"name\":\"EDITOR\",\"scope\":\"APPLICATION\",\"applicationId\":\"%s\"}".formatted(applicationId)).exchange().expectStatus().isCreated().expectBody().returnResult().getResponseBody());
        String profileId = idOf(client().post().uri("/api/v1/profiles").header("Authorization", bearer()).contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"name\":\"DOCENTES\",\"scope\":\"APPLICATION\",\"applicationId\":\"%s\"}".formatted(applicationId)).exchange().expectStatus().isCreated().expectBody().returnResult().getResponseBody());
        client().post().uri("/api/v1/profiles/" + profileId + "/roles").header("Authorization", bearer()).contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"roleId\":\"%s\"}".formatted(roleId)).exchange().expectStatus().isOk();
        client().get().uri(security("profiles/" + profileId + "/roles?page=0&size=1")).header("Authorization", bearer()).exchange()
                .expectStatus().isOk().expectBody().jsonPath("$.data.content[0].id").isEqualTo(roleId).jsonPath("$.data.total").isEqualTo(1).jsonPath("$.data.limit").isEqualTo(1);
    }

    private String security(String suffix) { return "/api/v1/applications/" + applicationId + "/security/" + suffix; }
    private String bearer() { return "Bearer " + TestJwtSupport.signedTokenWithMfaEvidence(TENANT, "test-subject"); }
    private String registerApplication(String name) {
        byte[] body = client().post().uri("/api/v1/applications")
                .header("Authorization", "Bearer " + TestJwtSupport.signedToken(TENANT, "test-subject"))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"name\":\"%s\",\"description\":\"\",\"baseUrl\":\"https://example.edu\"}".formatted(name))
                .exchange().expectStatus().isCreated().expectBody().returnResult().getResponseBody();
        JsonNode root = JSON.readTree(body);
        return root.path("data").path("id").asString();
    }
    private String idOf(byte[] body) throws Exception { return JSON.readTree(body).path("data").path("id").asString(); }
    private WebTestClient client() { return WebTestClient.bindToServer().baseUrl("http://localhost:" + port).build(); }
}
