package co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.primary.web;

import co.edu.uco.seguridad.AbstractSurrealDbIntegrationTest;
import co.edu.uco.seguridad.pdp.PdpApplication;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.commons.model.UserId;
import co.edu.uco.seguridad.pdp.identity.application.secondaryport.repository.SecurityUserRepository;
import co.edu.uco.seguridad.pdp.identity.domain.SecurityUser;
import co.edu.uco.seguridad.pdp.identity.domain.model.Email;
import co.edu.uco.seguridad.shared.security.TestJwtSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.UUID;

/**
 * Flujo HTTP completo sobre Netty, autenticado y contra una SurrealDB real — mismo patrón que
 * AssignmentHttpTests (HU-005). Toda la cadena es esqueleto todavía: se espera rojo por 500
 * (UnsupportedOperationException en el servidor), no los códigos de éxito que se afirman aquí.
 */
@SpringBootTest(classes = PdpApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProfileAssignmentHttpTests extends AbstractSurrealDbIntegrationTest {

    private static final String UCO = "universidad-uco";
    private static final ObjectMapper JSON = new ObjectMapper();

    @LocalServerPort
    int port;

    @Autowired
    SecurityUserRepository userRepository;

    private String prefix;

    @BeforeEach
    void freshPrefix() {
        prefix = "hu011-" + UUID.randomUUID().toString().substring(0, 8);
        // HU-015: SubjectUserIdLookupValidator necesita una identidad vinculada para el subject del
        // JWT de prueba — registerApplication() pasa por el registro gateado.
        linkTestIdentity(new TenantId(UCO), "test-subject");
    }

    @Test
    void assigning_a_profile_materializes_an_assignment_per_role() {
        String applicationId = registerApplication(UCO, prefix + "-app");
        String roleId = defineTenantScopedRole(UCO, prefix + "-rol");
        String profileId = defineTenantScopedProfile(UCO, prefix + "-perfil");
        addRoleToProfile(UCO, profileId, roleId);
        UserId userId = seedUser(UCO, prefix + "-user1@uco.edu.co");

        assignProfile(UCO, profileId, userId, applicationId)
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.data.profileId").isEqualTo(profileId)
                .jsonPath("$.data.generatedAssignmentIds.length()").isEqualTo(1);
    }

    @Test
    void refuses_assigning_the_same_profile_twice() {
        String applicationId = registerApplication(UCO, prefix + "-app-dup");
        String roleId = defineTenantScopedRole(UCO, prefix + "-rol-dup");
        String profileId = defineTenantScopedProfile(UCO, prefix + "-perfil-dup");
        addRoleToProfile(UCO, profileId, roleId);
        UserId userId = seedUser(UCO, prefix + "-user2@uco.edu.co");

        assignProfile(UCO, profileId, userId, applicationId).expectStatus().isCreated();

        assignProfile(UCO, profileId, userId, applicationId)
                .expectStatus().isEqualTo(org.springframework.http.HttpStatus.CONFLICT)
                .expectBody()
                .jsonPath("$.code").isEqualTo("PROFILE_ASSIGNMENT_ALREADY_ACTIVE");
    }

    private WebTestClient.ResponseSpec assignProfile(String tenant, String profileId, UserId userId, String applicationId) {
        return client().post().uri("/api/v1/profiles/" + profileId + "/assignments")
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

    private String defineTenantScopedProfile(String tenant, String name) {
        byte[] body = client().post().uri("/api/v1/profiles")
                .header("Authorization", "Bearer " + TestJwtSupport.signedToken(tenant, "test-subject"))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"name":"%s","scope":"TENANT"}""".formatted(name))
                .exchange()
                .expectStatus().isCreated()
                .expectBody().returnResult().getResponseBody();
        return JSON.readTree(body).path("data").path("id").asString();
    }

    private void addRoleToProfile(String tenant, String profileId, String roleId) {
        client().post().uri("/api/v1/profiles/" + profileId + "/roles")
                .header("Authorization", "Bearer " + TestJwtSupport.signedToken(tenant, "test-subject"))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"roleId":"%s"}""".formatted(roleId))
                .exchange()
                .expectStatus().isOk();
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
