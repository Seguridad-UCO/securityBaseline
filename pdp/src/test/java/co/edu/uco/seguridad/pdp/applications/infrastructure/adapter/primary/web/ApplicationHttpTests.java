package co.edu.uco.seguridad.pdp.applications.infrastructure.adapter.primary.web;

import co.edu.uco.seguridad.AbstractSurrealDbIntegrationTest;
import co.edu.uco.seguridad.pdp.PdpApplication;
import co.edu.uco.seguridad.shared.security.TestJwtSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.UUID;

/**
 * Flujo HTTP completo sobre Netty, autenticado y contra una SurrealDB real.
 *
 * <p>Es la evidencia end-to-end de los criterios 5 (envelope y códigos), 6 (parámetros de consulta),
 * 9 (traducción de excepciones) y 22 (cadena reactiva sin bloqueo), y sobre todo del aislamiento
 * entre inquilinos: ninguna prueba unitaria puede demostrar que el filtro por inquilino sobrevive a
 * la cadena entera, porque el tenant sale del token y no de un parámetro.
 */
@SpringBootTest(classes = PdpApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ApplicationHttpTests extends AbstractSurrealDbIntegrationTest {

    private static final String PATH = "/api/v1/applications";
    private static final String UCO = "universidad-uco";
    private static final String OTRO = "tenant-a";

    @LocalServerPort
    int port;

    private String prefix;

    @BeforeEach
    void registerApplications() {
        // Cada ejecución usa nombres únicos: la base es compartida entre pruebas y no se limpia.
        prefix = "hu001-" + UUID.randomUUID().toString().substring(0, 8);
        register(UCO, prefix + "-portal-estudiante");
        register(UCO, prefix + "-portal-docente");
        register(UCO, prefix + "-gestion-academica");
        register(OTRO, prefix + "-portal-ajeno");
    }

    @Test
    void lists_the_applications_of_the_authenticated_tenant() {
        get(UCO, "?name=" + prefix)
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo("APPLICATIONS_LISTED")
                .jsonPath("$.data.total").isEqualTo(3)
                .jsonPath("$.data.content.length()").isEqualTo(3);
    }

    @Test
    void filters_by_a_name_fragment_ignoring_case() {
        get(UCO, "?name=" + prefix + "-PORTAL")
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.total").isEqualTo(2);
    }

    @Test
    void reports_the_total_of_the_filter_while_returning_only_the_page() {
        get(UCO, "?name=" + prefix + "&page=0&size=2")
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.total").isEqualTo(3)
                .jsonPath("$.data.content.length()").isEqualTo(2)
                .jsonPath("$.data.limit").isEqualTo(2)
                .jsonPath("$.data.offset").isEqualTo(0);
    }

    @Test
    void the_second_page_returns_the_remaining_element() {
        get(UCO, "?name=" + prefix + "&page=1&size=2")
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.total").isEqualTo(3)
                .jsonPath("$.data.content.length()").isEqualTo(1)
                .jsonPath("$.data.page").isEqualTo(1);
    }

    @Test
    void a_range_window_converges_with_its_equivalent_page() {
        get(UCO, "?name=" + prefix + "&offset=2&limit=2")
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.content.length()").isEqualTo(1)
                .jsonPath("$.data.offset").isEqualTo(2);
    }

    @Test
    void refuses_an_ambiguous_window_naming_the_offending_field() {
        get(UCO, "?page=1&offset=5")
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo("CONFLICTING_REQUEST_PARAMETERS")
                .jsonPath("$.field").isEqualTo("page");
    }

    @Test
    void refuses_a_size_over_the_maximum() {
        get(UCO, "?size=101")
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo("MALFORMED_REQUEST_FIELD");
    }

    @Test
    void never_shows_the_applications_of_another_tenant() {
        get(OTRO, "?name=" + prefix)
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.total").isEqualTo(1)
                .jsonPath("$.data.content[0].tenantId").isEqualTo(OTRO);
    }

    @Test
    void an_empty_result_is_a_page_not_a_not_found() {
        get(UCO, "?name=" + prefix + "-no-existe")
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.total").isEqualTo(0)
                .jsonPath("$.data.content.length()").isEqualTo(0);
    }

    private WebTestClient.ResponseSpec get(String tenant, String query) {
        return client().get().uri(PATH + query)
                .header("Authorization", "Bearer " + TestJwtSupport.signedToken(tenant, "test-subject"))
                .exchange();
    }

    private void register(String tenant, String name) {
        client().post().uri(PATH)
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
