package co.edu.uco.seguridad.pep;

import co.edu.uco.seguridad.pep.fixture.FixtureServers;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PepHttpIntegrationTests {
    static final FixtureServers fixtures = new FixtureServers(0, 0);
    static final HttpClient client = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1)
            .followRedirects(HttpClient.Redirect.NEVER).build();
    @LocalServerPort
    int port;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        // El plano de datos del proxy no necesita habilitar el registro dinámico en esta fixture.
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
    }

    @BeforeEach
    void reset() {
        fixtures.mode.set("ALLOW");
        fixtures.forwarded.set(0);
        fixtures.evaluations.set(0);
        fixtures.healthy.set(true);
        fixtures.jwksHealthy.set(true);
    }

    @AfterAll
    static void stop() {
        fixtures.close();
    }

    String token() {
        return fixtures.token("protected-api", fixtures.issuer(), 300);
    }

    HttpRequest.Builder request(String path) {
        return HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).timeout(Duration.ofSeconds(10));
    }

    HttpResponse<byte[]> send(HttpRequest.Builder request) throws Exception {
        return client.send(request.build(), HttpResponse.BodyHandlers.ofByteArray());
    }

    HttpResponse<byte[]> get(String path) throws Exception {
        return send(request(path).header("Authorization", "Bearer " + token()).GET());
    }

    @Test
    @Order(1)
    void allow_forwards_once_with_canonical_contract_and_sanitized_headers() throws Exception {
        byte[] bytes = new byte[]{0, 1, 2, 3, -1};
        var result = send(request("/apps/demo/notes/77?q=a%20b&q=c")
                .header("Authorization", "Bearer " + token()).header("Cookie", "SESSION=secret")
                .header("X-Tenant-Id", "attacker").header("X-User-Id", "admin")
                .header("X-Forwarded-For", "evil").header("X-Request-Id", "spoofed")
                .header("X-Correlation-Id", "correlation-123").POST(HttpRequest.BodyPublishers.ofByteArray(bytes)));
        assertThat(result.statusCode()).as(new String(result.body(), java.nio.charset.StandardCharsets.UTF_8)).isEqualTo(200);
        assertThat(result.body()).isEqualTo(bytes);
        assertThat(fixtures.forwarded.get()).isEqualTo(1);
        assertThat(fixtures.evaluations.get()).isEqualTo(1);
        assertThat(fixtures.lastUri.get()).isEqualTo("/notes/77?q=a%20b&q=c");
        assertThat(fixtures.lastHeaders.get()).doesNotContainKeys("Authorization", "Cookie", "X-Tenant-Id", "X-User-Id");
        assertThat(fixtures.lastHeaders.get().get("X-Forwarded-For")).doesNotContain("evil");
        assertThat(result.headers().firstValue("X-Request-Id")).isNotEqualTo(Optional.of("spoofed"));
        assertThat(result.headers().firstValue("X-Decision-Id")).contains("fixture-decision");
        assertThat(result.headers().firstValue("Set-Cookie")).isEmpty();
        assertThat(fixtures.lastDecisionRequest.get()).containsEntry("version", "1").containsEntry("correlationId", "correlation-123");
        assertThat(fixtures.lastDecisionRequest.get().get("resource")).isEqualTo(Map.of("path", "/notes/77", "action", "POST"));
        assertThat(fixtures.lastDecisionRequest.get()).doesNotContainKeys("roles", "tenant", "token", "subject");
        var mapper = new tools.jackson.databind.ObjectMapper();
        java.nio.file.Files.writeString(java.nio.file.Path.of("target/observed-request.json"),
                mapper.writeValueAsString(fixtures.lastDecisionRequest.get()));
        java.nio.file.Files.writeString(java.nio.file.Path.of("target/observed-decision.json"),
                mapper.writeValueAsString(fixtures.lastDecisionResponse.get()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"DENY", "TOKEN_INVALID", "INDETERMINATE", "HTTP401", "HTTP500", "HTTP302", "EMPTY", "MALFORMED", "LARGE", "UNKNOWN", "MISSING", "MISMATCH", "OBLIGATION", "SLOW", "DUPLICATE"})
    void non_allow_never_reaches_backend(String mode) throws Exception {
        fixtures.mode.set(mode);
        var response = get("/apps/demo/resource");
        assertThat(response.statusCode()).isEqualTo(mode.equals("DENY") ? 403 : Set.of("TOKEN_INVALID", "HTTP401").contains(mode) ? 401 : 503);
        assertThat(fixtures.forwarded.get()).isZero();
        assertThat(new String(response.body(), java.nio.charset.StandardCharsets.UTF_8)).doesNotContain("fixture-policy", "Bearer", "Exception");
        if (Set.of("DENY", "TOKEN_INVALID", "INDETERMINATE").contains(mode)) {
            assertThat(response.headers().firstValue("X-Decision-Id")).contains("fixture-decision");
        }
    }

    @Test
    void invalid_identity_never_calls_pdp_or_backend() throws Exception {
        assertThat(send(request("/apps/demo/resource").GET()).statusCode()).isEqualTo(401);
        for (String jwt : List.of("invalid", fixtures.token("wrong", fixtures.issuer(), 300),
                fixtures.token("protected-api", "https://wrong", 300), fixtures.token("protected-api", fixtures.issuer(), -600))) {
            assertThat(send(request("/apps/demo/resource").header("Authorization", "Bearer " + jwt).GET()).statusCode()).isEqualTo(401);
        }
        assertThat(fixtures.evaluations.get()).isZero();
        assertThat(fixtures.forwarded.get()).isZero();
    }

    @Test
    void unavailable_jwks_is_503_when_a_key_must_be_fetched() throws Exception {
        fixtures.jwksHealthy.set(false);
        var result = send(request("/apps/demo/resource").header("Authorization", "Bearer "
                + fixtures.token("protected-api", fixtures.issuer(), 300, "uncached-key")).GET());
        assertThat(result.statusCode()).isEqualTo(503);
        assertThat(fixtures.evaluations.get()).isZero();
        assertThat(fixtures.forwarded.get()).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"/apps/demo/a/../b", "/apps/demo/a%2fb", "/apps/demo/a%252fb", "/apps/demo/a;b", "/apps/demo/a//b", "/apps/demo/%2e%2e/private"})
    void ambiguous_paths_cannot_be_authorized(String path) throws Exception {
        assertThat(get(path).statusCode()).isEqualTo(400);
        assertThat(fixtures.evaluations.get()).isZero();
        assertThat(fixtures.forwarded.get()).isZero();
    }

    @Test
    void unknown_route_cannot_select_target() throws Exception {
        assertThat(get("/apps/unknown/resource?url=http://evil").statusCode()).isEqualTo(404);
        assertThat(fixtures.evaluations.get()).isZero();
    }

    @Test
    void additive_response_fields_do_not_change_enforcement() throws Exception {
        fixtures.mode.set("ADDITIVE");
        assertThat(get("/apps/demo/resource").statusCode()).isEqualTo(200);
        assertThat(fixtures.forwarded.get()).isEqualTo(1);
    }

    @Test
    void additional_identity_headers_are_not_trusted_by_backend() throws Exception {
        assertThat(send(request("/apps/demo/resource").header("Authorization", "Bearer " + token())
                .header("X-Auth-Request-User", "admin").header("User", "admin")
                .header("Idempotency-Key", "business-key").GET()).statusCode()).isEqualTo(200);
        assertThat(fixtures.lastHeaders.get()).doesNotContainKeys("X-Auth-Request-User", "User");
        assertThat(fixtures.lastHeaders.get().get("Idempotency-Key")).containsExactly("business-key");
    }

    @Test
    void chunked_body_limit_aborts_stream() throws Exception {
        var response = send(request("/apps/demo/upload").header("Authorization", "Bearer " + token())
                .POST(HttpRequest.BodyPublishers.ofInputStream(() -> new java.io.ByteArrayInputStream(new byte[2048]))));
        assertThat(response.statusCode()).isEqualTo(413);
        // Unlike known Content-Length, bytes may already have reached the authorized destination.
        assertThat(fixtures.evaluations.get()).isEqualTo(1);
    }

    @Test
    void unsupported_protocol_and_duplicate_authorization_do_not_evaluate() throws Exception {
        assertThat(send(request("/apps/demo/resource").header("Authorization", "Bearer " + token())
                .header("Accept", "text/event-stream").GET()).statusCode()).isEqualTo(501);
        assertThat(send(request("/apps/demo/resource").header("Authorization", "Bearer " + token())
                .header("Authorization", "Bearer duplicate").GET()).statusCode()).isEqualTo(400);
        assertThat(fixtures.evaluations.get()).isZero();
    }

    @Test
    void finite_http_semantics_and_upstream_failures() throws Exception {
        assertThat(get("/apps/demo/teapot").statusCode()).isEqualTo(418);
        assertThat(get("/apps/demo/redirect").statusCode()).isEqualTo(302);
        assertThat(fixtures.forwarded.get()).isEqualTo(2);
        assertThat(get("/apps/demo/slow").statusCode()).isEqualTo(504);
        assertThat(get("/apps/demo/sse").statusCode()).isEqualTo(501);
    }

    @Test
    void oversized_known_body_rejected_before_evaluation() throws Exception {
        assertThat(send(request("/apps/demo/upload").header("Authorization", "Bearer " + token())
                .POST(HttpRequest.BodyPublishers.ofByteArray(new byte[1025]))).statusCode()).isEqualTo(413);
        assertThat(fixtures.evaluations.get()).isZero();
        assertThat(fixtures.forwarded.get()).isZero();
    }

    @Test
    void cors_preflight_is_local_and_scoped() throws Exception {
        var response = send(request("/apps/demo/resource").header("Origin", "http://localhost:5173")
                .header("Access-Control-Request-Method", "POST").header("Access-Control-Request-Headers", "Authorization")
                .method("OPTIONS", HttpRequest.BodyPublishers.noBody()));
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("Access-Control-Allow-Origin")).contains("http://localhost:5173");
        assertThat(send(request("/apps/demo/resource").header("Origin", "https://evil")
                .header("Access-Control-Request-Method", "POST").method("OPTIONS", HttpRequest.BodyPublishers.noBody())).statusCode()).isEqualTo(403);
        assertThat(fixtures.evaluations.get()).isZero();
        assertThat(fixtures.forwarded.get()).isZero();
    }

    @Test
    void readiness_tracks_pdp_without_breaking_liveness() throws Exception {
        var readiness = send(request("/actuator/health/readiness").GET());
        assertThat(readiness.statusCode()).as(new String(readiness.body(), java.nio.charset.StandardCharsets.UTF_8)).isEqualTo(200);
        fixtures.healthy.set(false);
        assertThat(send(request("/actuator/health/readiness").GET()).statusCode()).isEqualTo(503);
        assertThat(send(request("/actuator/health/liveness").GET()).statusCode()).isEqualTo(200);
    }

    @Test
    void concurrent_requests_keep_decisions_and_bodies_isolated() throws Exception {
        List<CompletableFuture<HttpResponse<byte[]>>> requests = new ArrayList<>();
        String jwt = token();
        for (int i = 0; i < 20; i++)
            requests.add(client.sendAsync(request("/apps/demo/item/" + i)
                    .header("Authorization", "Bearer " + jwt).header("X-Correlation-Id", "parallel-" + i)
                    .POST(HttpRequest.BodyPublishers.ofString("body-" + i)).build(), HttpResponse.BodyHandlers.ofByteArray()));
        for (int i = 0; i < requests.size(); i++) {
            var result = requests.get(i).get(10, TimeUnit.SECONDS);
            assertThat(result.statusCode()).isEqualTo(200);
            assertThat(new String(result.body(), java.nio.charset.StandardCharsets.UTF_8)).isEqualTo("body-" + i);
            assertThat(result.headers().firstValue("X-Correlation-Id")).contains("parallel-" + i);
        }
        assertThat(fixtures.forwarded.get()).isEqualTo(20);
    }
}
