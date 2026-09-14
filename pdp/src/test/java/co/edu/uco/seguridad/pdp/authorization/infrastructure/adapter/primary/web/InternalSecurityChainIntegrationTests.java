package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web;

import co.edu.uco.seguridad.AbstractSurrealDbIntegrationTest;
import co.edu.uco.seguridad.pdp.PdpApplication;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpServer;
import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Cadena de seguridad completa del canal interno (D2 mTLS + D5 evidencia JWT) sobre un socket real
 * — mismo patrón que {@code PdpTlsIntegrationTests} del PEP (T2, plan §9): certificados efímeros por
 * {@code openssl}, sin infraestructura adicional a la que ya exige {@link AbstractSurrealDbIntegrationTest}.
 * Dos certificados autofirmados e independientes (no una CA que firma dos hojas): el servidor confía
 * directamente en el certificado del cliente y viceversa, igual que hace el PEP — un autofirmado
 * puede ser su propio ancla de confianza cuando se añade tal cual al almacén.
 *
 * <p><b>Corrección sobre el plan:</b> la fila "aplicación inexistente → 403" del §6 contradice tanto
 * el contrato ({@code contracts/pep-pdp/v1/openapi.yaml}: toda decisión —incluida DENY— vive bajo la
 * respuesta {@code 200}; {@code 403} es "PEP service not admitted", no un resultado de decisión)
 * como el precedente ya construido y probado en {@code AuthorizeUseCaseImpl} y
 * {@code AuthorizationHttpTests.reports_tenant_mismatch_for_an_application_that_does_not_belong_to_the_tenant}
 * (200, nunca 403). Esta prueba sigue el contrato y el precedente: 200 con
 * {@code decision=DENY}/{@code reasonCode=TENANT_MISMATCH} para una aplicación que no existe.</p>
 */
@SpringBootTest(classes = PdpApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class InternalSecurityChainIntegrationTests extends AbstractSurrealDbIntegrationTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String CLIENT_SUBJECT = "CN=pep-test";
    private static final String ISSUER = "https://internal-evidence.test";
    private static final String AUDIENCE = "pdp-internal";

    @TempDir
    static Path certificates;

    private static Path serverCert;
    private static Path serverKey;
    private static Path clientCert;
    private static Path clientKey;
    private static HttpServer jwksServer;
    private static RSAPrivateKey signingKey;

    @LocalServerPort
    int port;

    @BeforeAll
    static void generateCertificatesAndJwks() throws Exception {
        serverCert = certificates.resolve("server-cert.pem");
        serverKey = certificates.resolve("server-key.pem");
        clientCert = certificates.resolve("client-cert.pem");
        clientKey = certificates.resolve("client-key.pem");
        generateSelfSignedCertificate(serverCert, serverKey, "/CN=localhost");
        generateSelfSignedCertificate(clientCert, clientKey, "/" + CLIENT_SUBJECT);

        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair pair = generator.generateKeyPair();
        signingKey = (RSAPrivateKey) pair.getPrivate();
        RSAKey publicJwk = new RSAKey.Builder((RSAPublicKey) pair.getPublic()).keyID("evidence-test-key").build();
        byte[] jwks = ("{\"keys\":[" + publicJwk.toJSONString() + "]}").getBytes(StandardCharsets.UTF_8);

        jwksServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        jwksServer.createContext("/jwks", exchange -> {
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, jwks.length);
            exchange.getResponseBody().write(jwks);
            exchange.close();
        });
        jwksServer.start();
    }

    @AfterAll
    static void stopJwksServer() {
        jwksServer.stop(0);
    }

    @DynamicPropertySource
    static void internalChannelProperties(DynamicPropertyRegistry registry) {
        registry.add("server.ssl.certificate", () -> "file:" + serverCert);
        registry.add("server.ssl.certificate-private-key", () -> "file:" + serverKey);
        registry.add("server.ssl.client-auth", () -> "want");
        registry.add("server.ssl.trust-certificate", () -> "file:" + clientCert);
        // El perfil por defecto (application.properties) trae mtls.enabled=false para conveniencia
        // de desarrollo local (PR #49) — esta prueba fija su propia postura explícitamente, sin
        // depender del default de ningún perfil.
        registry.add("pdp.security.internal.mtls.enabled", () -> "true");
        registry.add("pdp.security.internal.mtls.trust-certificate", clientCert::toString);
        registry.add("pdp.security.internal.mtls.allowed-subjects[0]", () -> CLIENT_SUBJECT);
        registry.add("pdp.security.internal.evidence.jwk-set-uri",
                () -> "http://127.0.0.1:" + jwksServer.getAddress().getPort() + "/jwks");
        registry.add("pdp.security.internal.evidence.issuer", () -> ISSUER);
        registry.add("pdp.security.internal.evidence.audience", () -> AUDIENCE);
    }

    @Test
    void mtls_and_valid_evidence_reach_the_controller_and_receive_a_structured_decision() {
        String body = requestBody(UUID.randomUUID().toString(), "req-1", "corr-1");

        byte[] response = client(clientCert, clientKey)
                .post().uri("/internal/v1/access-decisions")
                .header("Authorization", "Bearer " + signedEvidenceToken("evidence-subject"))
                .header("X-Request-Id", "req-1")
                .header("X-Correlation-Id", "corr-1")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchangeToMono(response2 -> {
                    assertThat(response2.statusCode().value()).isEqualTo(200);
                    return response2.bodyToMono(byte[].class);
                })
                .block(java.time.Duration.ofSeconds(10));

        JsonNode decision = JSON.readTree(response);
        assertThat(decision.path("decision").asString()).isEqualTo("DENY");
        assertThat(decision.path("reasonCode").asString()).isEqualTo("TENANT_MISMATCH");
        assertThat(decision.path("requestId").asString()).isEqualTo("req-1");
        assertThat(decision.path("correlationId").asString()).isEqualTo("corr-1");
    }

    @Test
    void rejects_without_a_client_certificate() {
        String body = requestBody(UUID.randomUUID().toString(), "req-2", "corr-2");

        Integer status = clientWithoutCertificate()
                .post().uri("/internal/v1/access-decisions")
                .header("Authorization", "Bearer " + signedEvidenceToken("evidence-subject"))
                .header("X-Request-Id", "req-2")
                .header("X-Correlation-Id", "corr-2")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchangeToMono(response -> reactor.core.publisher.Mono.just(response.statusCode().value()))
                .block(java.time.Duration.ofSeconds(10));

        assertThat(status).isEqualTo(403);
    }

    @Test
    void rejects_a_valid_certificate_with_evidence_missing_the_subject_claim() {
        String body = requestBody(UUID.randomUUID().toString(), "req-3", "corr-3");

        Integer status = client(clientCert, clientKey)
                .post().uri("/internal/v1/access-decisions")
                .header("Authorization", "Bearer " + signedEvidenceToken(null))
                .header("X-Request-Id", "req-3")
                .header("X-Correlation-Id", "corr-3")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchangeToMono(response -> reactor.core.publisher.Mono.just(response.statusCode().value()))
                .block(java.time.Duration.ofSeconds(10));

        assertThat(status).isEqualTo(401);
    }

    private String requestBody(String applicationId, String requestId, String correlationId) {
        return """
                {"version":"1","requestId":"%s","correlationId":"%s","timestamp":"%s",
                 "application":{"id":"%s","environment":"prod"},
                 "resource":{"path":"/estudiantes","action":"GET"},
                 "context":{"method":"GET","channel":"HTTP"}}
                """.formatted(requestId, correlationId, Instant.now(), applicationId);
    }

    private String signedEvidenceToken(String subject) {
        try {
            JWTClaimsSet.Builder claims = new JWTClaimsSet.Builder()
                    .issuer(ISSUER)
                    .audience(AUDIENCE)
                    .issueTime(Date.from(Instant.now()))
                    .expirationTime(Date.from(Instant.now().plusSeconds(300)));
            if (subject != null) {
                claims.subject(subject);
            }
            SignedJWT jwt = new SignedJWT(
                    new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("evidence-test-key").build(), claims.build());
            jwt.sign(new RSASSASigner(signingKey));
            return jwt.serialize();
        } catch (Exception cause) {
            throw new IllegalStateException("no se pudo firmar el token de evidencia de prueba", cause);
        }
    }

    private WebClient client(Path certificate, Path key) {
        return webClient(sslContext(certificate, key));
    }

    private WebClient clientWithoutCertificate() {
        return webClient(sslContext(null, null));
    }

    private WebClient webClient(SslContext sslContext) {
        HttpClient httpClient = HttpClient.create().secure(spec -> spec.sslContext(sslContext));
        return WebClient.builder()
                .baseUrl("https://localhost:" + port)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    private SslContext sslContext(Path certificate, Path key) {
        try {
            SslContextBuilder builder = SslContextBuilder.forClient()
                    .trustManager(serverCert.toFile())
                    .clientAuth(ClientAuth.NONE);
            if (certificate != null) {
                builder = builder.keyManager(certificate.toFile(), key.toFile());
            }
            return builder.build();
        } catch (Exception cause) {
            throw new IllegalStateException("no se pudo construir el SslContext de prueba", cause);
        }
    }

    private static void generateSelfSignedCertificate(Path certOut, Path keyOut, String subject) throws Exception {
        Process generation = new ProcessBuilder("openssl", "req", "-x509", "-newkey", "rsa:2048", "-nodes",
                "-keyout", keyOut.toString(), "-out", certOut.toString(), "-days", "1", "-subj", subject)
                .redirectErrorStream(true).redirectOutput(ProcessBuilder.Redirect.DISCARD).start();
        boolean finished = generation.waitFor(20, TimeUnit.SECONDS);
        if (!finished || generation.exitValue() != 0) {
            throw new IllegalStateException("openssl no pudo generar el certificado de prueba para " + subject);
        }
    }
}
