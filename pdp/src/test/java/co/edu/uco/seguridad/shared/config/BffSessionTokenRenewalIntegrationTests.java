package co.edu.uco.seguridad.shared.config;

import co.edu.uco.seguridad.shared.auth.model.BffSessionExpiredException;
import co.edu.uco.seguridad.shared.auth.model.TokenRenewalUnavailableException;
import co.edu.uco.seguridad.shared.auth.service.BffSessionTokenRenewer;
import co.edu.uco.seguridad.shared.security.SessionTokenRenewalProperties;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.server.WebSessionServerOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * HU-030: unico modo de probar el manager real de Spring (proveedor {@code refresh_token}, repositorio de
 * sesion y WebClient) contra HTTP. El cableado es el de produccion ({@code SessionTokenRenewalConfiguration})
 * sin levantar el contexto completo; Keycloak es un {@link HttpServer} local que cuenta los canjes y rota
 * el refresh token (un refresh ya consumido responde {@code invalid_grant}, como Keycloak).
 * Exige sockets de loopback.
 */
class BffSessionTokenRenewalIntegrationTests {

    private static final Authentication PRINCIPAL =
            new UsernamePasswordAuthenticationToken("keycloak-subject", null, List.of());
    private static final Duration SKEW = Duration.ofSeconds(60);

    private FakeKeycloak keycloak;

    @AfterEach
    void stop_keycloak() {
        if (keycloak != null) {
            keycloak.stop();
        }
    }

    @Test
    void an_expired_token_is_renewed_and_the_rotated_refresh_token_is_saved_in_the_session() {
        keycloak = FakeKeycloak.start();
        Session session = session(Duration.ofSeconds(5), expiresIn(-300));

        StepVerifier.create(session.renewer.validAccessToken(session.exchange, PRINCIPAL))
                .expectNext("access-1")
                .verifyComplete();

        assertThat(keycloak.exchanges.get()).isEqualTo(1);
        assertThat(keycloak.grantTypes).containsExactly("refresh_token");
        assertThat(keycloak.presentedRefreshTokens).containsExactly("refresh-0");
        OAuth2AuthorizedClient saved = session.savedClient();
        assertThat(saved.getAccessToken().getTokenValue()).isEqualTo("access-1");
        assertThat(saved.getRefreshToken().getTokenValue()).isEqualTo("refresh-1");
    }

    @Test
    void a_valid_token_is_returned_without_calling_keycloak() {
        keycloak = FakeKeycloak.start();
        Session session = session(Duration.ofSeconds(5), expiresIn(300));

        StepVerifier.create(session.renewer.validAccessToken(session.exchange, PRINCIPAL))
                .expectNext("stale-access")
                .verifyComplete();

        assertThat(keycloak.exchanges.get()).isZero();
    }

    @Test
    void a_token_inside_the_renewal_skew_is_renewed_even_though_it_has_not_expired() {
        keycloak = FakeKeycloak.start();
        Session session = session(Duration.ofSeconds(5), expiresIn(SKEW.getSeconds() / 2));

        StepVerifier.create(session.renewer.validAccessToken(session.exchange, PRINCIPAL))
                .expectNext("access-1")
                .verifyComplete();

        assertThat(keycloak.exchanges.get()).isEqualTo(1);
    }

    @Test
    void invalid_grant_means_the_session_expired_and_the_next_request_does_not_call_keycloak_again() {
        keycloak = FakeKeycloak.start();
        keycloak.acceptOnlyRefreshToken("a-different-refresh-token");
        Session session = session(Duration.ofSeconds(5), expiresIn(-300));

        StepVerifier.create(session.renewer.validAccessToken(session.exchange, PRINCIPAL))
                .expectError(BffSessionExpiredException.class)
                .verify();
        assertThat(keycloak.exchanges.get()).isEqualTo(1);

        StepVerifier.create(session.renewer.validAccessToken(session.exchange, PRINCIPAL))
                .expectError(BffSessionExpiredException.class)
                .verify();
        assertThat(keycloak.exchanges.get()).isEqualTo(1);
    }

    @Test
    void keycloak_down_means_unavailable_and_never_delivers_the_expired_token() {
        keycloak = FakeKeycloak.start();
        Session session = session(Duration.ofSeconds(5), expiresIn(-300));
        keycloak.stop();

        StepVerifier.create(session.renewer.validAccessToken(session.exchange, PRINCIPAL))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(TokenRenewalUnavailableException.class);
                    assertThat(String.valueOf(error.getMessage())).doesNotContain("stale-access");
                })
                .verify();
    }

    @Test
    void a_5xx_from_keycloak_means_unavailable() {
        keycloak = FakeKeycloak.start();
        keycloak.alwaysRespondWith(503);
        Session session = session(Duration.ofSeconds(5), expiresIn(-300));

        StepVerifier.create(session.renewer.validAccessToken(session.exchange, PRINCIPAL))
                .expectError(TokenRenewalUnavailableException.class)
                .verify();
    }

    @Test
    void a_keycloak_that_does_not_answer_within_the_timeout_means_unavailable() {
        keycloak = FakeKeycloak.start();
        keycloak.holdResponsesUntilReleased();
        Session session = session(Duration.ofMillis(300), expiresIn(-300));

        StepVerifier.create(session.renewer.validAccessToken(session.exchange, PRINCIPAL))
                .expectError(TokenRenewalUnavailableException.class)
                .verify(Duration.ofSeconds(10));
    }

    @Test
    void simultaneous_requests_of_one_session_produce_one_exchange_and_the_same_token() {
        keycloak = FakeKeycloak.start();
        keycloak.holdResponsesUntilReleased();
        Session session = session(Duration.ofSeconds(5), expiresIn(-300));

        StepVerifier.create(Flux.merge(Collections.nCopies(5, 0).stream()
                        .map(i -> session.renewer.validAccessToken(session.exchange, PRINCIPAL)).toList()).collectList())
                .then(keycloak::releaseResponses)
                .assertNext(tokens -> assertThat(tokens).hasSize(5).containsOnly("access-1"))
                .verifyComplete();

        assertThat(keycloak.exchanges.get()).isEqualTo(1);
        assertThat(session.savedClient().getRefreshToken().getTokenValue()).isEqualTo("refresh-1");
    }

    // ---- ayudas ----

    private static Instant expiresIn(long seconds) {
        return Instant.now().plusSeconds(seconds);
    }

    private Session session(Duration renewalTimeout, Instant accessTokenExpiresAt) {
        ClientRegistration registration = ClientRegistration.withRegistrationId("keycloak")
                .clientId("security-baseline-bff").clientSecret("secret")
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("http://localhost/callback")
                .authorizationUri(keycloak.baseUrl() + "/auth").tokenUri(keycloak.baseUrl() + "/token")
                .build();
        ServerOAuth2AuthorizedClientRepository repository = new WebSessionServerOAuth2AuthorizedClientRepository();
        BffSessionTokenRenewer renewer = new SessionTokenRenewalConfiguration().bffSessionTokenRenewer(
                new InMemoryReactiveClientRegistrationRepository(registration), repository,
                new SessionTokenRenewalProperties(SKEW, renewalTimeout), Instant::now);
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/internal/v1/bff-session-token").build());
        OAuth2AuthorizedClient initial = new OAuth2AuthorizedClient(registration, "keycloak-subject",
                new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, "stale-access",
                        accessTokenExpiresAt.minusSeconds(300), accessTokenExpiresAt),
                new OAuth2RefreshToken("refresh-0", accessTokenExpiresAt.minusSeconds(300)));
        repository.saveAuthorizedClient(initial, PRINCIPAL, exchange).block();
        return new Session(renewer, repository, exchange);
    }

    private record Session(BffSessionTokenRenewer renewer, ServerOAuth2AuthorizedClientRepository repository,
                           MockServerWebExchange exchange) {

        OAuth2AuthorizedClient savedClient() {
            return repository.<OAuth2AuthorizedClient>loadAuthorizedClient("keycloak", PRINCIPAL, exchange).block();
        }
    }

    /** Keycloak simulado: token endpoint que cuenta canjes, rota el refresh token y puede fallar o retener respuestas. */
    private static final class FakeKeycloak {
        final AtomicInteger exchanges = new AtomicInteger();
        final List<String> grantTypes = new CopyOnWriteArrayList<>();
        final List<String> presentedRefreshTokens = new CopyOnWriteArrayList<>();
        private final HttpServer server;
        private volatile String expectedRefreshToken = "refresh-0";
        private volatile int forcedStatus;
        private volatile CountDownLatch gate = new CountDownLatch(0);

        private FakeKeycloak(HttpServer server) {
            this.server = server;
        }

        static FakeKeycloak start() {
            try {
                HttpServer server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
                FakeKeycloak keycloak = new FakeKeycloak(server);
                server.createContext("/token", keycloak::handle);
                server.setExecutor(Executors.newCachedThreadPool());
                server.start();
                return keycloak;
            } catch (IOException e) {
                throw new AssertionError("no se pudo abrir el Keycloak simulado", e);
            }
        }

        String baseUrl() {
            return "http://127.0.0.1:" + server.getAddress().getPort();
        }

        void acceptOnlyRefreshToken(String refreshToken) {
            this.expectedRefreshToken = refreshToken;
        }

        void alwaysRespondWith(int status) {
            this.forcedStatus = status;
        }

        void holdResponsesUntilReleased() {
            this.gate = new CountDownLatch(1);
        }

        void releaseResponses() {
            gate.countDown();
        }

        void stop() {
            gate.countDown();
            server.stop(0);
        }

        private void handle(HttpExchange http) throws IOException {
            String body = new String(http.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            exchanges.incrementAndGet();
            String refreshToken = "";
            for (String pair : body.split("&")) {
                String[] keyValue = pair.split("=", 2);
                if (keyValue.length == 2 && keyValue[0].equals("grant_type")) {
                    grantTypes.add(keyValue[1]);
                }
                if (keyValue.length == 2 && keyValue[0].equals("refresh_token")) {
                    refreshToken = keyValue[1];
                    presentedRefreshTokens.add(refreshToken);
                }
            }
            try {
                gate.await(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            if (forcedStatus != 0) {
                reply(http, forcedStatus, "{\"error\":\"server_error\"}");
            } else if (!refreshToken.equals(expectedRefreshToken)) {
                reply(http, 400, "{\"error\":\"invalid_grant\",\"error_description\":\"Token is not active\"}");
            } else {
                int n = exchanges.get();
                expectedRefreshToken = "refresh-" + n;
                reply(http, 200, "{\"access_token\":\"access-" + n + "\",\"token_type\":\"Bearer\",\"expires_in\":300,"
                        + "\"refresh_token\":\"refresh-" + n + "\",\"refresh_expires_in\":1800}");
            }
        }

        private static void reply(HttpExchange http, int status, String json) throws IOException {
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            http.getResponseHeaders().add("Content-Type", "application/json");
            http.sendResponseHeaders(status, bytes.length);
            http.getResponseBody().write(bytes);
            http.close();
        }
    }
}
