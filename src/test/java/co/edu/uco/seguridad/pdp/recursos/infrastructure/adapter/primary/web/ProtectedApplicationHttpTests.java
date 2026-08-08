package co.edu.uco.seguridad.pdp.recursos.infrastructure.adapter.primary.web;

import co.edu.uco.seguridad.pdp.PdpApplication;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * La pila completa en Netty: filtro de correlación, controlador, mapeadores, interactor, caso de uso, reglas,
 * dummies y manejador de errores. Cada escenario usa su propio nombre de aplicación, por lo que el almacén
 * compartido en memoria no puede hacer que una prueba dependa de otra.
 */
@SpringBootTest(classes = PdpApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProtectedApplicationHttpTests {

    private static final String PATH = "/api/v1/protected-applications";

    @LocalServerPort
    int port;

    private WebTestClient client;

    @BeforeEach
    void setUp() {
        client = WebTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
    }

    @Test
    void registers_application_through_tenants_applications_and_resources_modules() {
        post(body("universidad-uco", "gestion-academica", "estudiantes", "consultar"))
                .header("X-Correlation-Id", "baseline-pdp-1")
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.code").isEqualTo("APPLICATION_REGISTERED")
                .jsonPath("$.data.resourceCode").isEqualTo("estudiantes")
                .jsonPath("$.data.applicationName").isEqualTo("gestion-academica")
                .jsonPath("$.correlationId").isEqualTo("baseline-pdp-1");
    }

    @Test
    void refuses_a_second_application_with_the_same_name_for_the_same_tenant() {
        post(body("universidad-uco", "portal-duplicado", "estudiantes", "consultar")).exchange().expectStatus().isCreated();

        post(body("universidad-uco", "portal-duplicado", "docentes", "consultar"))
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectBody()
                .jsonPath("$.code").isEqualTo("APPLICATION_ALREADY_EXISTS");
    }

    @Test
    void reports_a_missing_field_with_its_name_instead_of_a_framework_error() {
        post("{\"applicationName\":\"sin-tenant\",\"resourceCode\":\"estudiantes\",\"action\":\"consultar\"}")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo("MISSING_REQUEST_FIELD")
                .jsonPath("$.field").isEqualTo("tenantId");
    }

    @Test
    void reports_a_malformed_field_with_the_reason_stated_by_the_value_object() {
        post(body("universidad-uco", "codigo-invalido", "Estudiantes", "consultar"))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo("MALFORMED_REQUEST_FIELD")
                .jsonPath("$.field").isEqualTo("resourceCode")
                .jsonPath("$.detail").value(detail -> assertThat((String) detail).contains("kebab-case"));
    }

    @Test
    void refuses_a_reserved_application_name() {
        post(body("universidad-uco", "admin", "estudiantes", "consultar"))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo("RESERVED_APPLICATION_NAME");
    }

    @Test
    void refuses_an_unknown_tenant() {
        post(body("tenant-inexistente", "app-desconocida", "estudiantes", "consultar"))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo("TENANT_NOT_FOUND");
    }

    @Test
    void refuses_a_suspended_tenant() {
        post(body("colegio-suspendido", "app-suspendida", "estudiantes", "consultar"))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo("TENANT_NOT_ACTIVE");
    }

    @Test
    void queries_the_catalog_with_a_filter_and_an_explicit_range() {
        post(body("tenant-a", "catalogo-consulta", "matriculas", "consultar")).exchange().expectStatus().isCreated();

        client.get()
                .uri(PATH + "?tenantId=tenant-a&nameContains=catalogo&offset=0&limit=1")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo("CATALOG_QUERIED")
                .jsonPath("$.data.content[0].applicationName").isEqualTo("catalogo-consulta")
                .jsonPath("$.data.offset").isEqualTo(0)
                .jsonPath("$.data.limit").isEqualTo(1);
    }

    @Test
    void refuses_a_query_that_mixes_paging_with_ranges() {
        client.get()
                .uri(PATH + "?page=1&size=10&offset=0&limit=5")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo("CONFLICTING_REQUEST_PARAMETERS");
    }

    @Test
    void refuses_a_page_size_beyond_the_protective_limit() {
        client.get()
                .uri(PATH + "?size=500")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo("MALFORMED_REQUEST_FIELD");
    }

    @Test
    void echoes_correlation_headers_on_every_response() {
        client.get()
                .uri(PATH)
                .header("X-Request-Id", "req-42")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals("X-Request-Id", "req-42");
    }

    private WebTestClient.RequestHeadersSpec<?> post(String payload) {
        return client.post().uri(PATH).contentType(MediaType.APPLICATION_JSON).bodyValue(payload);
    }

    private static String body(String tenantId, String applicationName, String resourceCode, String action) {
        return """
                {"tenantId":"%s","applicationName":"%s","resourceCode":"%s","action":"%s"}
                """.formatted(tenantId, applicationName, resourceCode, action);
    }
}
