package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web;

import co.edu.uco.seguridad.pdp.commons.model.UserId;
import co.edu.uco.seguridad.shared.security.TestJwtSupport;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Pasos HTTP compartidos por las pruebas Bearer de HU-038: registrar aplicación y recurso, definir
 * un rol, concederle el recurso y asignarlo. El administrador es siempre {@code admin-subject}, con
 * evidencia MFA, igual que {@code AssignmentHttpTests}.
 */
final class BearerHttpFixture {

    static final String UCO = "universidad-uco";
    static final String ALLOW_BODY =
            "{\"result\":{\"effect\":\"ALLOW\",\"reasonCode\":\"POLICY_ALLOWED\",\"policyReferences\":[],\"obligations\":[]}}";
    static final String DENY_BODY =
            "{\"result\":{\"effect\":\"DENY\",\"reasonCode\":\"NO_APPLICABLE_POLICY\",\"policyReferences\":[],\"obligations\":[]}}";
    static final String ADMINISTRATION_DECISION_PATH = "/v1/data/security/administration/decision";
    static final String AUTHORIZATION_DECISION_PATH = "/v1/data/security/authorization/decision";

    private static final ObjectMapper JSON = new ObjectMapper();

    private final WebTestClient client;
    private final String adminSubject;

    BearerHttpFixture(WebTestClient client, String adminSubject) {
        this.client = client;
        this.adminSubject = adminSubject;
    }

    String registerApplication(String name) {
        byte[] body = client.post().uri("/api/v1/applications")
                .header("Authorization", "Bearer " + TestJwtSupport.signedToken(UCO, adminSubject))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"name":"%s","description":"","baseUrl":"https://example.com"}""".formatted(name))
                .exchange().expectStatus().isCreated().expectBody().returnResult().getResponseBody();
        return JSON.readTree(body).path("data").path("id").asString();
    }

    String registerResourceAndFindId(String applicationId, String path, String method) {
        client.post().uri("/api/v1/applications/" + applicationId + "/resources")
                .header("Authorization", "Bearer " + TestJwtSupport.signedTokenWithMfaEvidence(UCO, adminSubject))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"path":"%s","method":"%s"}""".formatted(path, method))
                .exchange().expectStatus().isCreated();
        byte[] body = client.get().uri("/api/v1/applications/" + applicationId + "/resources")
                .header("Authorization", "Bearer " + TestJwtSupport.signedToken(UCO, adminSubject))
                .exchange().expectStatus().isOk().expectBody().returnResult().getResponseBody();
        for (JsonNode resource : JSON.readTree(body).path("data")) {
            if (resource.path("path").asString().equals(path) && resource.path("method").asString().equals(method)) {
                return resource.path("id").asString();
            }
        }
        throw new IllegalStateException("recurso de prueba no encontrado: " + path + " " + method);
    }

    String defineTenantRole(String name) {
        byte[] body = client.post().uri("/api/v1/roles")
                .header("Authorization", "Bearer " + TestJwtSupport.signedTokenWithMfaEvidence(UCO, adminSubject))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"name":"%s","scope":"TENANT"}""".formatted(name))
                .exchange().expectStatus().isCreated().expectBody().returnResult().getResponseBody();
        return JSON.readTree(body).path("data").path("id").asString();
    }

    void grantResource(String roleId, String resourceId) {
        client.post().uri("/api/v1/roles/" + roleId + "/resources")
                .header("Authorization", "Bearer " + TestJwtSupport.signedTokenWithMfaEvidence(UCO, adminSubject))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"resourceId":"%s"}""".formatted(resourceId))
                .exchange().expectStatus().isOk();
    }

    void assignRole(String roleId, UserId userId, String applicationId) {
        client.post().uri("/api/v1/roles/" + roleId + "/assignments")
                .header("Authorization", "Bearer " + TestJwtSupport.signedTokenWithMfaEvidence(UCO, adminSubject))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"userId":"%s","applicationId":"%s"}""".formatted(userId.value(), applicationId))
                .exchange().expectStatus().isCreated();
    }

    WebTestClient.ResponseSpec authorize(String token, String applicationId, String path, String action) {
        return client.post().uri("/api/v1/authorize")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"applicationId":"%s","resourcePath":"%s","action":"%s"}""".formatted(applicationId, path, action))
                .exchange();
    }
}
