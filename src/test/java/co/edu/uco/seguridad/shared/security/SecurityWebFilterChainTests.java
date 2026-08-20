package co.edu.uco.seguridad.shared.security;

import co.edu.uco.seguridad.AbstractSurrealDbIntegrationTest;
import co.edu.uco.seguridad.pdp.PdpApplication;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * La frontera PEP como tal (ADR-0003): qué pasa antes de que una petición llegue al controlador.
 * Cubre los cuatro rechazos de autenticación y las dos rutas públicas.
 */
@SpringBootTest(classes = PdpApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SecurityWebFilterChainTests extends AbstractSurrealDbIntegrationTest {

    private static final String PATH = "/api/v1/applications";

    @LocalServerPort
    int port;

    private WebTestClient client() {
        return WebTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
    }

    @Test
    void refuses_a_request_without_a_token() {
        client().get().uri(PATH)
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.code").isEqualTo("UNAUTHORIZED");
    }

    @Test
    void refuses_a_token_with_a_tampered_signature() {
        String token = TestJwtSupport.signedToken("universidad-uco", "test-subject");
        String tampered = token.substring(0, token.lastIndexOf('.') + 1) + "tampered-signature";

        client().get().uri(PATH)
                .header("Authorization", "Bearer " + tampered)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void refuses_an_expired_token() {
        client().get().uri(PATH)
                .header("Authorization", "Bearer " + TestJwtSupport.expiredToken("universidad-uco", "test-subject"))
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void refuses_a_token_without_the_expected_audience() {
        client().get().uri(PATH)
                .header("Authorization", "Bearer " + TestJwtSupport.tokenWithoutAudience("universidad-uco", "test-subject"))
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void refuses_a_token_without_a_jti() {
        client().get().uri(PATH)
                .header("Authorization", "Bearer " + TestJwtSupport.tokenWithoutJti("universidad-uco", "test-subject"))
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void refuses_a_malformed_bearer_value() {
        client().get().uri(PATH)
                .header("Authorization", "Bearer not-a-jwt-at-all")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void accepts_a_well_formed_token_and_reaches_the_controller() {
        client().get().uri(PATH)
                .header("Authorization", "Bearer " + TestJwtSupport.signedToken("universidad-uco", "test-subject"))
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void actuator_health_stays_public() {
        client().get().uri("/actuator/health")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void actuator_info_stays_public() {
        client().get().uri("/actuator/info")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void register_still_rejects_an_unauthenticated_request() {
        client().post().uri(PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"name\":\"sin-token\",\"description\":\"\",\"baseUrl\":\"https://sin-token.example.com\"}")
                .exchange()
                .expectStatus().isUnauthorized();
    }
}
