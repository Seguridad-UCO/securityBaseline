package co.edu.uco.seguridad.pep.fixture;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * External HTTP fixtures, confined to test classpath. Never packaged in the PEP executable.
 */
public final class FixtureServers implements AutoCloseable {
    public final AtomicInteger evaluations = new AtomicInteger();
    public final AtomicInteger forwarded = new AtomicInteger();
    public final AtomicReference<String> mode = new AtomicReference<>("ALLOW");
    public final AtomicReference<Map<String, Object>> lastDecisionRequest = new AtomicReference<>();
    public final AtomicReference<Map<String, Object>> lastDecisionResponse = new AtomicReference<>();
    public final AtomicReference<Map<String, List<String>>> lastHeaders = new AtomicReference<>();
    public final AtomicReference<byte[]> lastBody = new AtomicReference<>();
    public final AtomicReference<String> lastUri = new AtomicReference<>();
    public final AtomicBoolean healthy = new AtomicBoolean(true);
    public final AtomicBoolean jwksHealthy = new AtomicBoolean(true);
    private final ObjectMapper mapper = new ObjectMapper();
    private final RSAKey key;
    private final DisposableServer pdp;
    private final DisposableServer app;
    private final String issuer;

    public FixtureServers(int pdpPort, int appPort) {
        try {
            key = new RSAKeyGenerator(2048).keyID("fixture-key").generate();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        pdp = HttpServer.create().host(System.getProperty("fixture.bind-host", "127.0.0.1")).port(pdpPort).handle((request, response) -> {
            String path = request.fullPath();
            if (path.equals("/jwks")) {
                return response.status(jwksHealthy.get() ? 200 : 503).header("Content-Type", "application/json")
                        .sendString(Mono.just(new JWKSet(key.toPublicJWK()).toString()));
            }
            if (path.equals("/actuator/health")) return response.status(healthy.get() ? 200 : 503).send();
            if (path.equals("/token"))
                return response.header("Content-Type", "text/plain").sendString(Mono.fromSupplier(() -> token("protected-api", issuer(), 300)));
            if (path.startsWith("/__fixture/mode/")) {
                mode.set(path.substring("/__fixture/mode/".length()));
                return response.send();
            }
            if (!path.equals("/internal/v1/access-decisions")) return response.status(404).send();
            evaluations.incrementAndGet();
            return request.receive().aggregate().asString().flatMap(json -> {
                @SuppressWarnings("unchecked") Map<String, Object> input = mapper.readValue(json, Map.class);
                lastDecisionRequest.set(input);
                String current = mode.get();
                if (current.equals("HTTP401")) return response.status(401).send().then();
                if (current.equals("HTTP500")) return response.status(500).send().then();
                if (current.equals("HTTP302")) return response.status(302).header("Location", "/token").send().then();
                if (current.equals("EMPTY")) return response.header("Content-Type", "application/json").send().then();
                if (current.equals("MALFORMED"))
                    return response.header("Content-Type", "application/json").sendString(Mono.just("{")).then();
                if (current.equals("LARGE"))
                    return response.header("Content-Type", "application/json").sendString(Mono.just("x".repeat(70000))).then();
                Map<String, Object> output = new LinkedHashMap<>();
                output.put("decision", Set.of("DENY", "INDETERMINATE", "UNKNOWN").contains(current) ? current : "ALLOW");
                output.put("decisionId", "fixture-decision");
                output.put("reasonCode", current.equals("TOKEN_INVALID") ? "TOKEN_INVALID" : "FIXTURE_" + current);
                if (current.equals("TOKEN_INVALID")) output.put("decision", "DENY");
                output.put("policyReferences", List.of(Map.of("id", "fixture-policy", "version", "1")));
                output.put("correlationId", current.equals("MISMATCH") ? "wrong" : input.get("correlationId"));
                output.put("requestId", input.get("requestId"));
                output.put("obligations", current.equals("OBLIGATION") ? List.of("mask") : List.of());
                if (current.equals("MISSING")) output.remove("decisionId");
                if (current.equals("ADDITIVE")) output.put("futureOptionalMetadata", "ignored");
                lastDecisionResponse.set(output);
                Mono<String> result = Mono.just(mapper.writeValueAsString(output));
                if (current.equals("DUPLICATE")) result = Mono.just(mapper.writeValueAsString(output)
                        .replace("\"decision\":\"ALLOW\"", "\"decision\":\"DENY\",\"decision\":\"ALLOW\""));
                if (current.equals("SLOW")) result = result.delayElement(Duration.ofSeconds(2));
                return response.header("Content-Type", "application/json").sendString(result).then();
            });
        }).bindNow();
        issuer = "http://localhost:" + pdp.port();
        app = HttpServer.create().host(System.getProperty("fixture.bind-host", "127.0.0.1")).port(appPort).handle((request, response) -> {
            forwarded.incrementAndGet();
            Map<String, List<String>> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            request.requestHeaders().forEach(entry -> headers.computeIfAbsent(entry.getKey(), k -> new ArrayList<>()).add(entry.getValue()));
            lastHeaders.set(headers);
            lastUri.set(request.uri());
            if (request.fullPath().equals("/slow"))
                return response.sendString(Mono.just("late").delayElement(Duration.ofSeconds(2)));
            if (request.fullPath().equals("/redirect"))
                return response.status(302).header("Location", "/elsewhere").send();
            if (request.fullPath().equals("/sse"))
                return response.header("Content-Type", "text/event-stream").sendString(Mono.just("data: event\n\n"));
            return request.receive().aggregate().asByteArray().defaultIfEmpty(new byte[0]).flatMap(bytes -> {
                lastBody.set(bytes);
                response.status(request.fullPath().equals("/teapot") ? 418 : 200);
                response.header("Content-Type", "application/octet-stream");
                response.header("X-Upstream", "fixture");
                response.header("X-Decision-Id", "spoofed");
                response.header("Set-Cookie", "upstream-session=secret");
                return response.sendByteArray(Mono.just(bytes.length == 0 ? "ok".getBytes(java.nio.charset.StandardCharsets.UTF_8) : bytes)).then();
            });
        }).bindNow();
    }

    public String issuer() {
        return issuer;
    }

    public String pdpUrl() {
        return "http://127.0.0.1:" + pdp.port();
    }

    public String appUrl() {
        return "http://127.0.0.1:" + app.port();
    }

    public int appPort() {
        return app.port();
    }

    public String token(String audience, String tokenIssuer, long seconds) {
        return token(audience, tokenIssuer, seconds, key.getKeyID());
    }

    public String token(String audience, String tokenIssuer, long seconds, String keyId) {
        try {
            var claims = new JWTClaimsSet.Builder().issuer(tokenIssuer).subject("fixture-user").audience(audience)
                    .issueTime(new Date()).expirationTime(Date.from(Instant.now().plusSeconds(seconds))).build();
            SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(keyId).build(), claims);
            jwt.sign(new RSASSASigner(key));
            return jwt.serialize();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void close() {
        pdp.disposeNow();
        app.disposeNow();
    }

    public static void main(String[] args) throws Exception {
        FixtureServers fixtures = new FixtureServers(18080, 18081);
        Runtime.getRuntime().addShutdownHook(new Thread(fixtures::close));
        System.out.println("LOCAL TEST FIXTURES ONLY: PDP/JWKS :18080, app :18081; GET /token returns a disposable test token.");
        Thread.currentThread().join();
    }
}
