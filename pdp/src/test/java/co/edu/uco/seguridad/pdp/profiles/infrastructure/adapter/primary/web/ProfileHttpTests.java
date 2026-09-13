package co.edu.uco.seguridad.pdp.profiles.infrastructure.adapter.primary.web;

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
 * Flujo HTTP completo sobre Netty, autenticado y contra una SurrealDB real — mismo patrón que
 * RoleHttpTests (HU-004). Toda la cadena (controller, interactor, caso de uso, repositorio) es
 * esqueleto todavía: se espera rojo por 500 (UnsupportedOperationException en el servidor), no los
 * códigos de éxito que se afirman aquí — confirmar eso es trabajo de la FASE 3 del tester, no de
 * este archivo.
 */
@SpringBootTest(classes = PdpApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProfileHttpTests extends AbstractSurrealDbIntegrationTest {

    private static final String UCO = "universidad-uco";

    @LocalServerPort
    int port;

    private String prefix;

    @BeforeEach
    void freshPrefix() {
        prefix = "hu011-" + UUID.randomUUID().toString().substring(0, 8);
    }

    @Test
    void defines_a_tenant_scoped_profile() {
        client().post().uri("/api/v1/profiles")
                .header("Authorization", "Bearer " + TestJwtSupport.signedToken(UCO, "test-subject"))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"name":"%s-coordinador","scope":"TENANT"}""".formatted(prefix))
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.data.scope").isEqualTo("TENANT")
                .jsonPath("$.data.tenantId").isEqualTo(UCO)
                .jsonPath("$.data.applicationId").doesNotExist();
    }

    @Test
    void refuses_a_global_scope_on_this_channel() {
        client().post().uri("/api/v1/profiles")
                .header("Authorization", "Bearer " + TestJwtSupport.signedToken(UCO, "test-subject"))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"name":"%s-super-perfil","scope":"GLOBAL"}""".formatted(prefix))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo("MALFORMED_REQUEST_FIELD")
                .jsonPath("$.field").isEqualTo("scope");
    }

    private WebTestClient client() {
        return WebTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
    }
}
