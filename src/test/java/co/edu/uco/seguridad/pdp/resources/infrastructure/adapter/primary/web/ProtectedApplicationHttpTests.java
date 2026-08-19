package co.edu.uco.seguridad.pdp.resources.infrastructure.adapter.primary.web;

import co.edu.uco.seguridad.AbstractSurrealDbIntegrationTest;
import co.edu.uco.seguridad.pdp.PdpApplication;
import co.edu.uco.seguridad.shared.security.TestJwtSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * La pila completa en Netty: filtro de correlación, seguridad JWT, controlador, mapeadores,
 * interactor, caso de uso, reglas, dummies y manejador de errores. Cada escenario usa su propio
 * nombre de aplicación, por lo que el almacén compartido en memoria no puede hacer que una prueba
 * dependa de otra.
 *
 * <p>Desde ADR-0003 el tenant ya no viaja en el cuerpo ni en la query: cada petición lleva un
 * {@code Authorization: Bearer} con el tenant como claim. Las pruebas de autenticación en sí
 * (401 sin token/token inválido/expirado) viven en {@code SecurityWebFilterChainTests}; este
 * archivo se queda con el flujo de negocio, ahora autenticado.</p>
 */
@SpringBootTest(classes = PdpApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProtectedApplicationHttpTests extends AbstractSurrealDbIntegrationTest {

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
        post("universidad-uco", body("gestion-academica", "estudiantes", "consultar"))
                .header("X-Correlation-Id", "baseline-pdp-1")
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.code").isEqualTo("APPLICATION_REGISTERED")
                .jsonPath("$.data.resourceCode").isEqualTo("estudiantes")
                .jsonPath("$.data.applicationName").isEqualTo("gestion-academica")
                .jsonPath("$.data.tenantId").isEqualTo("universidad-uco")
                .jsonPath("$.correlationId").isEqualTo("baseline-pdp-1");
    }

    @Test
    void refuses_a_second_application_with_the_same_name_for_the_same_tenant() {
        post("universidad-uco", body("portal-duplicado", "estudiantes", "consultar"))
                .exchange().expectStatus().isCreated();

        post("universidad-uco", body("portal-duplicado", "docentes", "consultar"))
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectBody()
                .jsonPath("$.code").isEqualTo("APPLICATION_ALREADY_EXISTS");
    }

    @Test
    void reports_a_missing_field_with_its_name_instead_of_a_framework_error() {
        post("universidad-uco", "{\"resourceCode\":\"estudiantes\",\"action\":\"consultar\"}")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo("MISSING_REQUEST_FIELD")
                .jsonPath("$.field").isEqualTo("applicationName");
    }

    @Test
    void reports_a_malformed_field_with_the_reason_stated_by_the_value_object() {
        post("universidad-uco", body("codigo-invalido", "Estudiantes", "consultar"))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo("MALFORMED_REQUEST_FIELD")
                .jsonPath("$.field").isEqualTo("resourceCode")
                .jsonPath("$.detail").value(detail -> assertThat((String) detail).contains("kebab-case"));
    }

    @Test
    void refuses_a_reserved_application_name() {
        post("universidad-uco", body("admin", "estudiantes", "consultar"))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo("RESERVED_APPLICATION_NAME");
    }

    @Test
    void refuses_a_token_whose_tenant_claim_names_an_unknown_tenant() {
        post("tenant-inexistente", body("app-desconocida", "estudiantes", "consultar"))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo("TENANT_NOT_FOUND");
    }

    @Test
    void refuses_a_token_whose_tenant_claim_names_a_suspended_tenant() {
        post("colegio-suspendido", body("app-suspendida", "estudiantes", "consultar"))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo("TENANT_NOT_ACTIVE");
    }

    @Test
    void queries_the_catalog_with_a_filter_and_an_explicit_range() {
        post("tenant-a", body("catalogo-consulta", "matriculas", "consultar")).exchange().expectStatus().isCreated();

        client.get()
                .uri(PATH + "?nameContains=catalogo&offset=0&limit=1")
                .header("Authorization", bearer("tenant-a"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo("CATALOG_QUERIED")
                .jsonPath("$.data.content[0].applicationName").isEqualTo("catalogo-consulta")
                .jsonPath("$.data.offset").isEqualTo(0)
                .jsonPath("$.data.limit").isEqualTo(1);
    }

    @Test
    void a_tenant_never_sees_another_tenants_catalog() {
        post("universidad-uco", body("solo-uco", "matriculas", "consultar")).exchange().expectStatus().isCreated();

        client.get()
                .uri(PATH + "?nameContains=solo-uco")
                .header("Authorization", bearer("tenant-a"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.content").isEmpty();
    }

    @Test
    void refuses_a_query_that_mixes_paging_with_ranges() {
        client.get()
                .uri(PATH + "?page=1&size=10&offset=0&limit=5")
                .header("Authorization", bearer("universidad-uco"))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo("CONFLICTING_REQUEST_PARAMETERS");
    }

    @Test
    void refuses_a_page_size_beyond_the_protective_limit() {
        client.get()
                .uri(PATH + "?size=500")
                .header("Authorization", bearer("universidad-uco"))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo("MALFORMED_REQUEST_FIELD");
    }

    @Test
    void echoes_correlation_headers_on_every_response() {
        client.get()
                .uri(PATH)
                .header("Authorization", bearer("universidad-uco"))
                .header("X-Request-Id", "req-42")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals("X-Request-Id", "req-42");
    }

    private WebTestClient.RequestHeadersSpec<?> post(String tenant, String payload) {
        return client.post().uri(PATH)
                .header("Authorization", bearer(tenant))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload);
    }

    private static String bearer(String tenant) {
        return "Bearer " + TestJwtSupport.signedToken(tenant, "test-subject");
    }

    private static String body(String applicationName, String resourceCode, String action) {
        return """
                {"applicationName":"%s","resourceCode":"%s","action":"%s"}
                """.formatted(applicationName, resourceCode, action);
    }
}
