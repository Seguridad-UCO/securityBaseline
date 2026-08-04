package co.edu.uco.seguridad.applications.infrastructure.web;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.junit.jupiter.api.BeforeEach;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProtectedApplicationHttpTests {
    @LocalServerPort int port;
    private WebTestClient client;

    @BeforeEach void connectClient() { client = WebTestClient.bindToServer().baseUrl("http://localhost:" + port).build(); }

    @Test void registers_and_lists_a_protected_application_with_correlation() {
        client.post().uri("/api/v1/protected-applications")
            .header("X-Request-Id", "request-test-1").header("X-Correlation-Id", "correlation-test-1")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("{\"name\":\"Billing API\",\"tenantId\":\"tenant-a\",\"resource\":\"/invoices\"}")
            .exchange().expectStatus().isCreated()
            .expectHeader().valueEquals("X-Request-Id", "request-test-1")
            .expectBody().jsonPath("$.code").isEqualTo("APPLICATION_REGISTERED")
            .jsonPath("$.data.tenantId").isEqualTo("tenant-a")
            .jsonPath("$.correlationId").isEqualTo("correlation-test-1");

        client.get().uri("/api/v1/protected-applications?tenantId=tenant-a&offset=0&limit=1")
            .exchange().expectStatus().isOk().expectBody()
            .jsonPath("$.code").isEqualTo("APPLICATIONS_FOUND")
            .jsonPath("$.data.total").isEqualTo(1)
            .jsonPath("$.data.content[0].resource").isEqualTo("/invoices");
    }
}
