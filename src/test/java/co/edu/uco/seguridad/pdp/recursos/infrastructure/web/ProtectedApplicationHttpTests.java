package co.edu.uco.seguridad.pdp.recursos.infrastructure.web;

import co.edu.uco.seguridad.pdp.PdpApplication;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(classes = PdpApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProtectedApplicationHttpTests {
    @LocalServerPort int port; private WebTestClient client;
    @BeforeEach void setup() { client = WebTestClient.bindToServer().baseUrl("http://localhost:" + port).build(); }
    @Test void registers_application_through_tenants_applications_and_resources_modules() {
        client.post().uri("/api/v1/protected-applications").header("X-Correlation-Id", "baseline-pdp-1").contentType(MediaType.APPLICATION_JSON)
            .bodyValue("{\"tenantId\":\"universidad-uco\",\"applicationName\":\"gestion-academica\",\"resourceCode\":\"estudiantes\",\"action\":\"consultar\"}")
            .exchange().expectStatus().isCreated().expectBody().jsonPath("$.code").isEqualTo("APPLICATION_REGISTERED").jsonPath("$.data.resourceCode").isEqualTo("estudiantes").jsonPath("$.correlationId").isEqualTo("baseline-pdp-1");
    }
}
