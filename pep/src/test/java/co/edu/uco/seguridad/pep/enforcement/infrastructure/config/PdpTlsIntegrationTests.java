package co.edu.uco.seguridad.pep.enforcement.infrastructure.config;

import co.edu.uco.seguridad.pep.commons.*;
import co.edu.uco.seguridad.pep.enforcement.application.port.primary.dto.request.EnforceAccessRequest;
import co.edu.uco.seguridad.pep.enforcement.infrastructure.properties.PdpClientProperties;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.netty.handler.ssl.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import reactor.core.publisher.Mono;
import reactor.netty.http.server.HttpServer;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.nio.file.Path;
import java.time.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;

class PdpTlsIntegrationTests {
    @TempDir Path directory;

    @Test void mtls_validates_client_certificate_ca_and_server_hostname() throws Exception {
        Path cert = directory.resolve("cert.pem"), key = directory.resolve("key.pem");
        Process generation = new ProcessBuilder("openssl", "req", "-x509", "-newkey", "rsa:2048",
                "-nodes", "-keyout", key.toString(), "-out", cert.toString(), "-days", "1", "-subj", "/CN=localhost")
                .redirectError(directory.resolve("openssl.log").toFile()).redirectOutput(ProcessBuilder.Redirect.DISCARD).start();
        assertThat(generation.waitFor(20, TimeUnit.SECONDS)).isTrue();
        assertThat(generation.exitValue()).isZero();
        var ssl = SslContextBuilder.forServer(cert.toFile(), key.toFile()).trustManager(cert.toFile())
                .clientAuth(ClientAuth.REQUIRE).build();
        var mapper = new ObjectMapper();
        AtomicInteger evaluated = new AtomicInteger();
        var server = HttpServer.create().host("127.0.0.1").port(0).secure(spec -> spec.sslContext(ssl))
                .handle((request, response) -> request.receive().aggregate().asString().flatMap(json -> {
                    evaluated.incrementAndGet();
                    @SuppressWarnings("unchecked") Map<String, Object> input = mapper.readValue(json, Map.class);
                    Map<String, Object> decision = Map.of("decision", "ALLOW", "decisionId", "tls-decision",
                            "reasonCode", "FIXTURE_ALLOW", "policyReferences", List.of(),
                            "requestId", input.get("requestId"), "correlationId", input.get("correlationId"));
                    return response.header("Content-Type", "application/json")
                            .sendString(Mono.just(mapper.writeValueAsString(decision))).then();
                })).bindNow();
        try {
            var config = new EnforcementConfiguration();
            var input = new AccessRequest("1", UUID.randomUUID().toString(), "tls-test", Instant.now(),
                    new AccessRequest.Application("app", "test"), new AccessRequest.Resource("/resource", "GET"),
                    new AccessRequest.Context("GET", "HTTP"));
            var request = new EnforceAccessRequest(input, new IdentityEvidence("fixture-evidence"));
            var valid = properties("https://localhost:" + server.port(), false, cert, cert, key);
            var client = config.decisionPort(config.pdpWebClient(valid, org.springframework.web.reactive.function.client.WebClient.builder()), valid, new SimpleMeterRegistry());
            assertThat(client.execute(request).block(Duration.ofSeconds(5)).decision()).isEqualTo(AccessDecision.Decision.ALLOW);
            assertThat(evaluated.get()).isEqualTo(1);

            var noCertificate = properties("https://localhost:" + server.port(), true, cert, null, null);
            var anonymous = config.decisionPort(config.pdpWebClient(noCertificate, org.springframework.web.reactive.function.client.WebClient.builder()), noCertificate, new SimpleMeterRegistry());
            assertThatThrownBy(() -> anonymous.execute(request).block(Duration.ofSeconds(5))).isInstanceOf(EnforcementFailure.class);

            var wrongHost = properties("https://127.0.0.1:" + server.port(), false, cert, cert, key);
            var wrong = config.decisionPort(config.pdpWebClient(wrongHost, org.springframework.web.reactive.function.client.WebClient.builder()), wrongHost, new SimpleMeterRegistry());
            assertThatThrownBy(() -> wrong.execute(request).block(Duration.ofSeconds(5))).isInstanceOf(EnforcementFailure.class);

            var untrusted = properties("https://localhost:" + server.port(), true, null, cert, key);
            var foreign = config.decisionPort(config.pdpWebClient(untrusted, org.springframework.web.reactive.function.client.WebClient.builder()), untrusted, new SimpleMeterRegistry());
            assertThatThrownBy(() -> foreign.execute(request).block(Duration.ofSeconds(5))).isInstanceOf(EnforcementFailure.class);
            assertThat(evaluated.get()).isEqualTo(1);
        } finally {
            server.disposeNow();
        }
    }

    private static PdpClientProperties properties(String url, boolean development, Path ca, Path cert, Path key) {
        return new PdpClientProperties(URI.create(url), development, Duration.ofSeconds(1), Duration.ofSeconds(2),
                65536, ca, cert, key);
    }
}
