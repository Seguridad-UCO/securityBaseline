package co.edu.uco.seguridad.pdp.resources.infrastructure.adapter.primary.web;

import co.edu.uco.seguridad.AbstractSurrealDbIntegrationTest;
import co.edu.uco.seguridad.pdp.PdpApplication;
import co.edu.uco.seguridad.shared.security.TestJwtSupport;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

/**
 * Flujo HTTP completo de la saga (HU-010), autenticado y contra SurrealDB real. Solo el camino
 * feliz: no hay un rechazo de negocio real y reproducible tras crear la aplicación (el recurso
 * inicial nunca puede colisionar con uno existente, ver PLAN-HU-010.md §0) — la compensación se
 * prueba a nivel de caso de uso, con colaboradores falsos.
 */
@SpringBootTest(classes = PdpApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ApplicationWithInitialResourceHttpTests extends AbstractSurrealDbIntegrationTest {

    private static final String PATH = "/api/v1/applications/with-initial-resource";
    private static final String UCO = "universidad-uco";
    private static final ObjectMapper JSON = new ObjectMapper();

    @LocalServerPort
    int port;

    @Test
    void registers_the_application_and_its_initial_resource_in_one_call() {
        String name = "hu010-" + UUID.randomUUID().toString().substring(0, 8);

        byte[] body = client().post().uri(PATH)
                .header("Authorization", "Bearer " + TestJwtSupport.signedToken(UCO, "test-subject"))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"name":"%s","description":"","baseUrl":"https://example.com","resourcePath":"/estudiantes","resourceMethod":"GET"}"""
                        .formatted(name))
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.data.credential").exists()
                .jsonPath("$.data.applicationName").isEqualTo(name)
                .jsonPath("$.data.resourcePath").isEqualTo("/estudiantes")
                .jsonPath("$.data.resourceMethod").isEqualTo("GET")
                .returnResult().getResponseBody();

        String applicationId = JSON.readTree(body).path("data").path("applicationId").asString();

        // La respuesta ya confirma la forma; esta segunda llamada confirma que el recurso quedó
        // persistido de verdad, no solo devuelto en la respuesta del primer POST.
        client().get().uri("/api/v1/applications/" + applicationId + "/resources")
                .header("Authorization", "Bearer " + TestJwtSupport.signedToken(UCO, "test-subject"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.length()").isEqualTo(1)
                .jsonPath("$.data[0].path").isEqualTo("/estudiantes")
                .jsonPath("$.data[0].method").isEqualTo("GET");
    }

    private WebTestClient client() {
        return WebTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
    }
}
