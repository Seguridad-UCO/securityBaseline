package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web;

import co.edu.uco.seguridad.AbstractSurrealDbIntegrationTest;
import co.edu.uco.seguridad.pdp.PdpApplication;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.secondary.policy.OpaFixtureServer;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.commons.model.UserId;
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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SU-01 de punta a punta: el catálogo de usuarios solo lo ve un actor de inquilino, solo el de su
 * inquilino, y nadie mueve usuarios entre inquilinos. Antes, cualquier token válido listaba usuarios de
 * todos los inquilinos y reasignaba el inquilino de cualquier persona.
 */
@SpringBootTest(classes = PdpApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UserAdministrationHttpTests extends AbstractSurrealDbIntegrationTest {

    private static final String UCO = "universidad-uco";
    private static final String OTHER = "tenant-a";
    private static final String ADMIN_PATH = "/v1/data/security/administration/decision";
    private static final String ALLOW = "{\"result\":{\"effect\":\"ALLOW\",\"reasonCode\":\"POLICY_ALLOWED\",\"policyReferences\":[],\"obligations\":[]}}";
    private static final String DENY = "{\"result\":{\"effect\":\"DENY\",\"reasonCode\":\"POLICY_DENIED\",\"policyReferences\":[],\"obligations\":[]}}";
    private static final ObjectMapper JSON = new ObjectMapper();

    private static OpaFixtureServer opa;

    @LocalServerPort
    int port;

    private UserId sameTenantUser;
    private UserId otherTenantUser;

    @BeforeAll
    static void startOpa() throws Exception {
        opa = OpaFixtureServer.start();
    }

    @AfterAll
    static void stopOpa() {
        opa.stop();
    }

    @DynamicPropertySource
    static void opaProperties(DynamicPropertyRegistry registry) {
        registry.add("pdp.opa.base-url", () -> opa.baseUrl());
    }

    @BeforeEach
    void seed() {
        opa.respondWithForPath(ADMIN_PATH, 200, ALLOW);
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        linkTestIdentity(new TenantId(UCO), "test-subject");
        sameTenantUser = linkTestIdentity(new TenantId(UCO), "su01-same-" + suffix);
        otherTenantUser = linkTestIdentity(new TenantId(OTHER), "su01-other-" + suffix);
        registerApplication("su01-" + suffix);
    }

    @Test
    void a_principal_that_administers_no_application_can_neither_list_nor_reassign() {
        opa.respondWithForPath(ADMIN_PATH, 200, DENY);

        client().get().uri("/api/v1/users").header("Authorization", bearer()).exchange()
                .expectStatus().isForbidden()
                .expectBody().jsonPath("$.code").isEqualTo("NOT_AUTHORIZED_TO_ADMINISTER_TENANT_CATALOG");
        assign(sameTenantUser, UCO)
                .expectStatus().isForbidden()
                .expectBody().jsonPath("$.code").isEqualTo("NOT_AUTHORIZED_TO_ADMINISTER_TENANT_CATALOG");
    }

    @Test
    void a_tenant_actor_only_sees_the_users_of_their_own_tenant() {
        byte[] body = client().get().uri("/api/v1/users").header("Authorization", bearer()).exchange()
                .expectStatus().isOk().expectBody().returnResult().getResponseBody();

        JsonNode users = JSON.readTree(body).path("data");
        assertThat(users.valueStream().map(user -> user.path("tenantId").asString())).isNotEmpty().containsOnly(UCO);
        assertThat(users.valueStream().map(user -> user.path("id").asString()))
                .contains(sameTenantUser.value().toString())
                .doesNotContain(otherTenantUser.value().toString());
    }

    @Test
    void nobody_moves_a_user_between_tenants_in_either_direction() {
        assign(sameTenantUser, OTHER)
                .expectStatus().isForbidden()
                .expectBody().jsonPath("$.code").isEqualTo("NOT_AUTHORIZED_TO_ADMINISTER_TENANT_CATALOG");
        assign(otherTenantUser, UCO)
                .expectStatus().isForbidden()
                .expectBody().jsonPath("$.code").isEqualTo("NOT_AUTHORIZED_TO_ADMINISTER_TENANT_CATALOG");
    }

    @Test
    void a_tenant_actor_can_still_reassign_within_their_tenant() {
        assign(sameTenantUser, UCO)
                .expectStatus().isOk()
                .expectBody().jsonPath("$.data.tenantId").isEqualTo(UCO);
    }

    private WebTestClient.ResponseSpec assign(UserId user, String tenantCode) {
        return client().put().uri("/api/v1/users/" + user.value() + "/tenant")
                .header("Authorization", bearer()).contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"tenantCode\":\"%s\"}".formatted(tenantCode))
                .exchange();
    }

    private String bearer() {
        return "Bearer " + TestJwtSupport.signedToken(UCO, "test-subject");
    }

    private void registerApplication(String name) {
        client().post().uri("/api/v1/applications")
                .header("Authorization", bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"name\":\"%s\",\"description\":\"\",\"baseUrl\":\"https://example.edu\"}".formatted(name))
                .exchange().expectStatus().isCreated();
    }

    private WebTestClient client() {
        return WebTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
    }
}
