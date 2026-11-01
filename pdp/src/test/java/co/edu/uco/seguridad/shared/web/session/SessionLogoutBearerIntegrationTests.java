package co.edu.uco.seguridad.shared.web.session;

import co.edu.uco.seguridad.AbstractRedisIntegrationTest;
import co.edu.uco.seguridad.AbstractSurrealDbIntegrationTest;
import co.edu.uco.seguridad.pdp.PdpApplication;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.commons.model.UserId;
import co.edu.uco.seguridad.shared.security.revocation.TokenRevocationPort;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * HU-028 (replanteada, ADR-034), prueba E2E con un bearer REAL: JWT RS256 firmado con un JWKS local
 * y validado por la cadena {@code /api/v1/**} del perfil {@code keycloak} (la única que envuelve el
 * decoder con {@code RevocationAwareJwtDecoder}). Redis real; SurrealDB real solo para vincular las
 * identidades de prueba. Es la quinta clase con contexto de Spring: lo justifica el criterio de
 * aceptación (el mismo JWT debe recibir 401 tras el logout).
 *
 * <p>El descubrimiento OIDC del perfil {@code keycloak} (eager al arrancar) se resuelve sirviendo
 * {@code /.well-known/openid-configuration} desde el mismo {@link HttpServer} que publica el JWKS
 * (opción B del plan, §11-2). Los {@code iat} son explícitos: nunca se duerme el hilo.</p>
 *
 * <p>El 503 se simula con un decorador {@code @Primary} del puerto que falla solo las escrituras:
 * con Redis totalmente caído, un bearer recibe 401 en el decoder (fail-closed, ADR-025).</p>
 */
@SpringBootTest(classes = PdpApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("keycloak")
@Import(SessionLogoutBearerIntegrationTests.FailableRevocationConfiguration.class)
class SessionLogoutBearerIntegrationTests extends AbstractSurrealDbIntegrationTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String REALM_PATH = "/realms/hu028";
    private static final String AUDIENCE = "security-baseline-pdp";
    private static final String TENANT = "universidad-uco";
    private static final String CLIENT_A = "hu028-app-a";
    private static final String CLIENT_B = "hu028-app-b";
    private static final AtomicBoolean REVOCATION_WRITES_FAIL = new AtomicBoolean(false);

    private static HttpServer idpServer;
    private static RSAPrivateKey signingKey;
    private static String issuer;

    @LocalServerPort
    int port;

    @BeforeAll
    static void startLocalIdentityProvider() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair pair = generator.generateKeyPair();
        signingKey = (RSAPrivateKey) pair.getPrivate();
        RSAKey publicJwk = new RSAKey.Builder((RSAPublicKey) pair.getPublic()).keyID("hu028-key").build();
        byte[] jwks = ("{\"keys\":[" + publicJwk.toJSONString() + "]}").getBytes(StandardCharsets.UTF_8);

        idpServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        idpServer.setExecutor(Executors.newCachedThreadPool(runnable -> {
            Thread thread = new Thread(runnable);
            thread.setDaemon(true);
            return thread;
        }));
        issuer = "http://127.0.0.1:" + idpServer.getAddress().getPort() + REALM_PATH;
        byte[] discovery = ("""
                {"issuer":"%1$s",
                 "authorization_endpoint":"%1$s/protocol/openid-connect/auth",
                 "token_endpoint":"%1$s/protocol/openid-connect/token",
                 "jwks_uri":"%1$s/protocol/openid-connect/certs",
                 "response_types_supported":["code"],
                 "grant_types_supported":["authorization_code","refresh_token"],
                 "subject_types_supported":["public"],
                 "id_token_signing_alg_values_supported":["RS256"],
                 "token_endpoint_auth_methods_supported":["client_secret_basic"]}
                """.formatted(issuer)).getBytes(StandardCharsets.UTF_8);
        serve("/.well-known/openid-configuration", discovery);
        serve("/protocol/openid-connect/certs", jwks);
        idpServer.start();
    }

    private static void serve(String path, byte[] body) {
        idpServer.createContext(REALM_PATH + path, exchange -> {
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
    }

    @AfterAll
    static void stopLocalIdentityProvider() {
        idpServer.stop(0);
    }

    @AfterEach
    void restoreRevocationWrites() {
        REVOCATION_WRITES_FAIL.set(false);
    }

    @DynamicPropertySource
    static void keycloakProfileAgainstTheLocalIdentityProvider(DynamicPropertyRegistry registry) {
        registry.add("spring.security.oauth2.client.registration.keycloak.client-secret", () -> "hu028-test-secret");
        registry.add("spring.security.oauth2.client.provider.keycloak.issuer-uri", () -> issuer);
        registry.add("pdp.security.jwt.jwk-set-uri", () -> issuer + "/protocol/openid-connect/certs");
        registry.add("pdp.security.jwt.issuer", () -> issuer);
        registry.add("pdp.security.jwt.audience", () -> AUDIENCE);
        registry.add("spring.data.redis.host", AbstractRedisIntegrationTest.REDIS::getHost);
        registry.add("spring.data.redis.port", () -> AbstractRedisIntegrationTest.REDIS.getMappedPort(6379));
    }

    // ---- DELETE /api/v1/session con bearer ----

    @Test
    void delete_with_a_bearer_answers_204_and_the_same_jwt_is_rejected_afterwards() {
        String subject = newLinkedSubject();
        String token = signedToken(subject, CLIENT_A, UUID.randomUUID().toString(), Instant.now().minusSeconds(60));
        assertThat(get(token).status()).isEqualTo(200);

        Response logout = delete(token);

        assertThat(logout.status()).isEqualTo(204);
        assertThat(logout.body()).isEmpty();
        assertUnauthorizedEnvelope(get(token));
    }

    @Test
    void an_expired_bearer_is_rejected_with_the_platform_unauthorized_envelope() {
        String subject = newLinkedSubject();
        String expired = signedToken(subject, CLIENT_A, UUID.randomUUID().toString(), Instant.now().minusSeconds(7200),
                Instant.now().minusSeconds(3600));

        assertUnauthorizedEnvelope(get(expired));
    }

    @Test
    void a_bearer_with_a_tampered_signature_is_rejected_with_the_platform_unauthorized_envelope() {
        String subject = newLinkedSubject();
        String token = signedToken(subject, CLIENT_A, UUID.randomUUID().toString(), Instant.now().minusSeconds(60));
        String tampered = token.substring(0, token.length() - 4) + (token.endsWith("AAAA") ? "BBBB" : "AAAA");

        assertUnauthorizedEnvelope(get(tampered));
    }

    @Test
    void delete_also_revokes_another_older_token_of_the_same_subject_and_client() {
        String subject = newLinkedSubject();
        String loggingOut = signedToken(subject, CLIENT_A, UUID.randomUUID().toString(), Instant.now().minusSeconds(60));
        String sameClientOlder = signedToken(subject, CLIENT_A, UUID.randomUUID().toString(), Instant.now().minusSeconds(600));
        assertThat(get(sameClientOlder).status()).isEqualTo(200);

        assertThat(delete(loggingOut).status()).isEqualTo(204);

        assertThat(get(sameClientOlder).status()).isEqualTo(401);
    }

    @Test
    void delete_does_not_affect_a_token_of_another_client_of_the_same_subject() {
        String subject = newLinkedSubject();
        String clientA = signedToken(subject, CLIENT_A, UUID.randomUUID().toString(), Instant.now().minusSeconds(60));
        String clientB = signedToken(subject, CLIENT_B, UUID.randomUUID().toString(), Instant.now().minusSeconds(60));

        assertThat(delete(clientA).status()).isEqualTo(204);

        assertThat(get(clientB).status()).isEqualTo(200);
    }

    @Test
    void delete_with_a_token_without_azp_revokes_only_that_jti() {
        String subject = newLinkedSubject();
        String withoutAzp = signedToken(subject, null, UUID.randomUUID().toString(), Instant.now().minusSeconds(60));
        String otherWithoutAzp = signedToken(subject, null, UUID.randomUUID().toString(), Instant.now().minusSeconds(120));
        String withAzp = signedToken(subject, CLIENT_A, UUID.randomUUID().toString(), Instant.now().minusSeconds(60));

        assertThat(delete(withoutAzp).status()).isEqualTo(204);

        assertThat(get(withoutAzp).status()).isEqualTo(401);
        assertThat(get(otherWithoutAzp).status()).isEqualTo(200);
        assertThat(get(withAzp).status()).isEqualTo(200);
    }

    @Test
    void delete_does_not_affect_tokens_of_another_subject_of_the_same_client() {
        String subject = newLinkedSubject();
        String otherSubject = newLinkedSubject();
        String own = signedToken(subject, CLIENT_A, UUID.randomUUID().toString(), Instant.now().minusSeconds(60));
        String foreign = signedToken(otherSubject, CLIENT_A, UUID.randomUUID().toString(), Instant.now().minusSeconds(60));

        assertThat(delete(own).status()).isEqualTo(204);

        assertThat(get(foreign).status()).isEqualTo(200);
    }

    @Test
    void a_token_of_the_same_subject_and_client_issued_after_the_logout_is_still_valid() {
        String subject = newLinkedSubject();
        String loggingOut = signedToken(subject, CLIENT_A, UUID.randomUUID().toString(), Instant.now().minusSeconds(60));
        String issuedAfterLogout = signedToken(subject, CLIENT_A, UUID.randomUUID().toString(), Instant.now().plusSeconds(3600));

        assertThat(delete(loggingOut).status()).isEqualTo(204);

        assertThat(get(issuedAfterLogout).status()).isEqualTo(200);
    }

    @Test
    void delete_answers_503_when_the_revocation_write_fails_and_the_token_stays_valid() {
        String subject = newLinkedSubject();
        String token = signedToken(subject, CLIENT_A, UUID.randomUUID().toString(), Instant.now().minusSeconds(60));
        assertThat(get(token).status()).isEqualTo(200);

        REVOCATION_WRITES_FAIL.set(true);
        Response failed = delete(token);
        REVOCATION_WRITES_FAIL.set(false);

        assertThat(failed.status()).isEqualTo(503);
        assertThat(JSON.readTree(failed.body()).path("code").asString()).isEqualTo("DEPENDENCY_UNAVAILABLE");
        assertThat(get(token).status()).isEqualTo(200);
    }

    @Test
    void delete_without_a_bearer_is_rejected_with_401() {
        Response response = client().delete().uri("/api/v1/session")
                .exchangeToMono(Response::from).block(Duration.ofSeconds(10));

        assertThat(response).isNotNull();
        assertUnauthorizedEnvelope(response);
    }

    // ---- ayudas ----

    /** 401 con el sobre de {@code ApiAuthenticationEntryPoint}: problem+json y {@code code=UNAUTHORIZED}. */
    private static void assertUnauthorizedEnvelope(Response response) {
        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo(401);
        assertThat(response.contentType()).startsWith("application/problem+json");
        assertThat(JSON.readTree(response.body()).path("code").asString()).isEqualTo("UNAUTHORIZED");
    }

    private record Response(int status, String body, String contentType) {
        static Mono<Response> from(org.springframework.web.reactive.function.client.ClientResponse response) {
            return response.bodyToMono(String.class).defaultIfEmpty("")
                    .map(body -> new Response(response.statusCode().value(), body,
                            response.headers().contentType().map(Object::toString).orElse("")));
        }
    }

    private Response get(String token) {
        return client().get().uri("/api/v1/session")
                .header("Authorization", "Bearer " + token)
                .exchangeToMono(Response::from).block(Duration.ofSeconds(10));
    }

    private Response delete(String token) {
        return client().delete().uri("/api/v1/session")
                .header("Authorization", "Bearer " + token)
                .exchangeToMono(Response::from).block(Duration.ofSeconds(10));
    }

    private WebClient client() {
        return WebClient.builder().baseUrl("http://localhost:" + port).build();
    }

    /** Subject único por llamada: la base compartida no se limpia entre pruebas ni corridas. */
    private static String newLinkedSubject() {
        String subject = "hu028-" + UUID.randomUUID();
        UserId linked = linkTestIdentity(new TenantId(TENANT), subject);
        assertThat(linked).isNotNull();
        return subject;
    }

    /** {@code azp} nulo omite el claim; {@code issuedAt} explícito (pasado o futuro), sin dormir. */
    private static String signedToken(String subject, String authorizedParty, String tokenId, Instant issuedAt) {
        return signedToken(subject, authorizedParty, tokenId, issuedAt, Instant.now().plusSeconds(7200));
    }

    private static String signedToken(String subject, String authorizedParty, String tokenId, Instant issuedAt,
                                      Instant expiresAt) {
        try {
            JWTClaimsSet.Builder claims = new JWTClaimsSet.Builder()
                    .issuer(issuer)
                    .audience(AUDIENCE)
                    .subject(subject)
                    .jwtID(tokenId)
                    .claim("tenant", TENANT)
                    .issueTime(Date.from(issuedAt))
                    .expirationTime(Date.from(expiresAt));
            if (authorizedParty != null) {
                claims.claim("azp", authorizedParty);
            }
            SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("hu028-key").build(),
                    claims.build());
            jwt.sign(new RSASSASigner(signingKey));
            return jwt.serialize();
        } catch (Exception cause) {
            throw new IllegalStateException("no se pudo firmar el token de prueba", cause);
        }
    }

    /**
     * Decorador del puerto real: las lecturas ({@code is*}) siguen siendo reales; las escrituras
     * fallan como lo haría Redis al perder la conexión entre decodificar y escribir.
     */
    @TestConfiguration
    static class FailableRevocationConfiguration {

        @Bean
        @Primary
        TokenRevocationPort failableTokenRevocationPort(@Qualifier("tokenRevocationPort") TokenRevocationPort real) {
            return new TokenRevocationPort() {
                @Override
                public Mono<Void> revokeAllSince(UserId subject, Instant since) {
                    return failingWriteOr(() -> real.revokeAllSince(subject, since));
                }

                @Override
                public Mono<Void> revokeToken(UserId subject, String tokenId, Instant expiresAt) {
                    return failingWriteOr(() -> real.revokeToken(subject, tokenId, expiresAt));
                }

                @Override
                public Mono<Boolean> isRevoked(UserId subject, Instant issuedAt) {
                    return real.isRevoked(subject, issuedAt);
                }

                @Override
                public Mono<Boolean> isTokenRevoked(UserId subject, String tokenId) {
                    return real.isTokenRevoked(subject, tokenId);
                }

                @Override
                public Mono<Void> revokeClientSince(UserId subject, String clientId, Instant since) {
                    return failingWriteOr(() -> real.revokeClientSince(subject, clientId, since));
                }

                @Override
                public Mono<Boolean> isClientRevoked(UserId subject, String clientId, Instant issuedAt) {
                    return real.isClientRevoked(subject, clientId, issuedAt);
                }

                private Mono<Void> failingWriteOr(java.util.function.Supplier<Mono<Void>> write) {
                    return Mono.defer(() -> REVOCATION_WRITES_FAIL.get()
                            ? Mono.error(new RedisSystemException("Redis unreachable", new RuntimeException("write refused")))
                            : write.get());
                }
            };
        }
    }
}
