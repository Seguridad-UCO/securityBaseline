package co.edu.uco.seguridad.shared.config;

import co.edu.uco.seguridad.AbstractSurrealDbIntegrationTest;
import co.edu.uco.seguridad.pdp.PdpApplication;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * ADR-022: el futuro frontend llama la API desde otro origen. Sin un origen en la lista blanca de
 * {@code pdp.security.cors.allowed-origins}, ningún preflight pasa — el default de este perfil
 * (application.properties) es un par de puertos de dev-server comunes, no un origen real.
 */
@SpringBootTest(classes = PdpApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CorsConfigurationTests extends AbstractSurrealDbIntegrationTest {

    private static final String PATH = "/api/v1/protected-applications";
    private static final String ALLOWED_ORIGIN = "http://localhost:5173";
    private static final String DISALLOWED_ORIGIN = "http://evil.example.com";

    @LocalServerPort
    int port;

    private WebTestClient client() {
        return WebTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
    }

    @Test
    void preflight_from_the_allowed_origin_is_accepted() {
        client().method(HttpMethod.OPTIONS).uri(PATH)
                .header("Origin", ALLOWED_ORIGIN)
                .header("Access-Control-Request-Method", "GET")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals("Access-Control-Allow-Origin", ALLOWED_ORIGIN);
    }

    @Test
    void preflight_from_a_disallowed_origin_is_rejected() {
        client().method(HttpMethod.OPTIONS).uri(PATH)
                .header("Origin", DISALLOWED_ORIGIN)
                .header("Access-Control-Request-Method", "GET")
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void a_real_response_to_the_allowed_origin_carries_the_cors_header() {
        client().get().uri("/actuator/health")
                .header("Origin", ALLOWED_ORIGIN)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals("Access-Control-Allow-Origin", ALLOWED_ORIGIN);
    }
}
