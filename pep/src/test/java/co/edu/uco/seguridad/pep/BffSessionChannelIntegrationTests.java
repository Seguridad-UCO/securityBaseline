package co.edu.uco.seguridad.pep;

import co.edu.uco.seguridad.pep.fixture.FixtureServers;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * HU-031: la indisponibilidad del canal BFF del PDP llega al cliente como 503, la sesión muerta como 401,
 * y en ningún caso se evalúa ni se reenvía nada sin token de usuario.
 * <p>
 * Se ejercita por el endpoint embebido: el proxy rechaza con 401 las peticiones sin Bearer en la cadena
 * de seguridad, antes de llegar al resolvedor.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BffSessionChannelIntegrationTests {
    static final FixtureServers fixtures = new FixtureServers(0, 0);
    static final HttpClient client = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1)
            .followRedirects(HttpClient.Redirect.NEVER).build();
    @LocalServerPort
    int port;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("pep.integration.enabled", () -> false);
        registry.add("pep.ingress.issuer", fixtures::issuer);
        registry.add("pep.ingress.jwks-uri", () -> fixtures.pdpUrl() + "/jwks");
        registry.add("pep.ingress.allow-insecure-http", () -> true);
        registry.add("pep.ingress.allowed-origins[0]", () -> "http://localhost:5173");
        registry.add("pep.ingress.routes[0].prefix", () -> "/apps/demo");
        registry.add("pep.ingress.routes[0].application-id", () -> "demo-app");
        registry.add("pep.ingress.routes[0].environment", () -> "test");
        registry.add("pep.ingress.routes[0].target", fixtures::appUrl);
        registry.add("pep.ingress.routes[0].audiences[0]", () -> "protected-api");
        registry.add("pep.ingress.max-body-bytes", () -> 1024);
        registry.add("pep.ingress.requests-per-second", () -> 1000);
        registry.add("pep.ingress.burst", () -> 1000);
        registry.add("pep.pdp.allow-insecure-http", () -> true);
        registry.add("pep.pdp.base-url", fixtures::pdpUrl);
        registry.add("pep.pdp.timeout", () -> "500ms");
        registry.add("pep.proxy.timeout", () -> "500ms");
        registry.add("pep.pdp.service-identity.token-uri", () -> fixtures.pdpUrl() + "/service-token");
        registry.add("pep.pdp.service-identity.client-id", () -> "fixture-pep");
        registry.add("pep.pdp.service-identity.client-secret", () -> "fixture-secret");
    }

    @BeforeEach
    void reset() {
        fixtures.mode.set("ALLOW");
        fixtures.bffMode.set("OK");
        fixtures.forwarded.set(0);
        fixtures.evaluations.set(0);
        fixtures.bffSessionRequests.set(0);
    }

    @AfterAll
    static void stop() {
        fixtures.close();
    }

    @Test
    void pdp_503_on_the_bff_channel_reaches_the_client_as_503_with_the_platform_envelope() throws Exception {
        fixtures.bffMode.set("HTTP503");

        HttpResponse<byte[]> response = embeddedDecisionWithBffCookie();

        assertThat(response.statusCode()).isEqualTo(503);
        assertThat(response.headers().firstValue("Content-Type").orElse("")).startsWith("application/problem+json");
        assertThat(response.headers().firstValue("Retry-After")).contains("5");
        String body = new String(response.body(), StandardCharsets.UTF_8);
        assertThat(body).contains("\"code\":\"BFF_SESSION_UNAVAILABLE\"").contains("requestId")
                .doesNotContain("Exception");
        assertThat(fixtures.bffSessionRequests.get()).isEqualTo(1);
        assertThat(fixtures.evaluations.get()).isZero();
        assertThat(fixtures.forwarded.get()).isZero();
    }

    @Test
    void pdp_401_on_the_bff_channel_reaches_the_client_as_401_and_nothing_is_evaluated() throws Exception {
        fixtures.bffMode.set("HTTP401");

        HttpResponse<byte[]> response = embeddedDecisionWithBffCookie();

        assertThat(response.statusCode()).isEqualTo(401);
        assertThat(new String(response.body(), StandardCharsets.UTF_8)).contains("\"code\":\"TOKEN_INVALID\"");
        assertThat(fixtures.bffSessionRequests.get()).isEqualTo(1);
        assertThat(fixtures.evaluations.get()).isZero();
        assertThat(fixtures.forwarded.get()).isZero();
    }

    private HttpResponse<byte[]> embeddedDecisionWithBffCookie() throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port
                        + "/internal/v1/embedded-access-decisions")).timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .header("X-Application-Credential", "fixture-application-secret")
                .header("Cookie", "SECURITY_BASELINE_SESSION=session-123")
                .POST(HttpRequest.BodyPublishers.ofString(
                        "{\"applicationName\":\"demo-app\",\"environment\":\"test\",\"path\":\"/resource\",\"method\":\"GET\"}"))
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofByteArray());
    }
}
