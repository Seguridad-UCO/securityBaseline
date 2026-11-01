package co.edu.uco.seguridad.shared.auth.service;

import co.edu.uco.seguridad.shared.auth.model.BffSessionExpiredException;
import co.edu.uco.seguridad.shared.auth.model.TokenRenewalUnavailableException;
import co.edu.uco.seguridad.shared.port.TimeProvider;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.ClientAuthorizationException;
import org.springframework.security.oauth2.client.ClientAuthorizationRequiredException;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.test.StepVerifier;

import java.lang.reflect.Field;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.IntFunction;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * HU-030 (ADR-033 §2): el renovador entrega siempre un access token vigente, colapsa las renovaciones
 * simultaneas de una sesion en un solo canje y traduce los fallos a 401 / 503. El manager de Spring se
 * dobla con un fake que reproduce su contrato: devuelve el cliente de la sesion si sigue vigente y, si no,
 * "canjea" (cuenta un canje y guarda la rotacion en la sesion).
 */
class BffSessionTokenRenewerTests {

    private static final Instant NOW = Instant.parse("2026-10-28T12:00:00Z");
    private static final TimeProvider FIXED_TIME = () -> NOW;
    private static final Duration RENEWAL_TIMEOUT = Duration.ofSeconds(5);
    private static final Authentication PRINCIPAL =
            new UsernamePasswordAuthenticationToken("keycloak-subject", null, List.of());

    // ---- ruta feliz ----

    @Test
    void returns_the_current_token_without_exchanging_when_it_is_still_valid() {
        FakeSessionManager manager = new FakeSessionManager(client("access-0", "refresh-0", NOW.plusSeconds(300)),
                attempt -> Mono.error(new AssertionError("no debe canjear con un token vigente")));
        BffSessionTokenRenewer renewer = renewer(manager);

        StepVerifier.create(renewer.validAccessToken(exchange(), PRINCIPAL))
                .expectNext("access-0")
                .verifyComplete();

        assertThat(manager.exchanges.get()).isZero();
        assertThat(inFlightEntries(renewer)).isZero();
    }

    @Test
    void renews_an_expired_token_and_saves_the_rotation_in_the_session() {
        FakeSessionManager manager = new FakeSessionManager(expiredClient(), attempt -> Mono.just(rotated(attempt)));
        BffSessionTokenRenewer renewer = renewer(manager);

        StepVerifier.create(renewer.validAccessToken(exchange(), PRINCIPAL))
                .expectNext("access-1")
                .verifyComplete();

        assertThat(manager.exchanges.get()).isEqualTo(1);
        OAuth2AuthorizedClient saved = manager.session.get();
        assertThat(saved.getAccessToken().getTokenValue()).isEqualTo("access-1");
        assertThat(saved.getRefreshToken().getTokenValue()).isEqualTo("refresh-1");
    }

    @Test
    void authorizes_the_keycloak_registration_with_the_given_principal_and_exchange() {
        FakeSessionManager manager = new FakeSessionManager(expiredClient(), attempt -> Mono.just(rotated(attempt)));
        MockServerWebExchange exchange = exchange();

        StepVerifier.create(renewer(manager).validAccessToken(exchange, PRINCIPAL))
                .expectNext("access-1")
                .verifyComplete();

        OAuth2AuthorizeRequest request = manager.lastRequest.get();
        assertThat(request.getClientRegistrationId()).isEqualTo("keycloak");
        assertThat(request.getPrincipal().getName()).isEqualTo("keycloak-subject");
        assertThat((Object) request.getAttribute(ServerWebExchange.class.getName())).isSameAs(exchange);
    }

    // ---- fallos: 401 ----

    @Test
    void invalid_grant_means_the_session_expired() {
        assertExpired(authorizationError("invalid_grant"));
    }

    @Test
    void invalid_token_means_the_session_expired() {
        assertExpired(authorizationError("invalid_token"));
    }

    @Test
    void a_session_without_authorized_client_means_the_session_expired() {
        assertExpired(new ClientAuthorizationRequiredException("keycloak"));
    }

    @Test
    void a_client_returned_still_expired_means_the_session_expired_and_never_leaks_the_old_token() {
        // Sin refresh token el proveedor de Spring devuelve el mismo cliente vencido.
        FakeSessionManager manager = new FakeSessionManager(expiredClient(), attempt -> Mono.just(expiredClient()));

        StepVerifier.create(renewer(manager).validAccessToken(exchange(), PRINCIPAL))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(BffSessionExpiredException.class);
                    assertThat(String.valueOf(error.getMessage())).doesNotContain("access-expired");
                })
                .verify();
    }

    @Test
    void a_manager_that_returns_nothing_means_the_session_expired() {
        FakeSessionManager manager = new FakeSessionManager(expiredClient(), attempt -> Mono.empty());
        BffSessionTokenRenewer renewer = renewer(manager);

        StepVerifier.create(renewer.validAccessToken(exchange(), PRINCIPAL))
                .expectError(BffSessionExpiredException.class)
                .verify();

        assertThat(inFlightEntries(renewer)).isZero();
    }

    // ---- fallos: 503 ----

    @Test
    void a_network_failure_means_keycloak_is_unavailable() {
        assertUnavailable(new IllegalStateException("Connection refused",
                new java.net.ConnectException("Connection refused")));
    }

    @Test
    void a_server_error_from_keycloak_means_keycloak_is_unavailable() {
        assertUnavailable(authorizationError("server_error"));
    }

    @Test
    void an_invalid_client_error_means_keycloak_is_unavailable_not_an_expired_session() {
        assertUnavailable(authorizationError("invalid_client"));
    }

    @Test
    void a_renewal_that_exceeds_the_timeout_means_keycloak_is_unavailable() {
        FakeSessionManager manager = new FakeSessionManager(expiredClient(), attempt -> Mono.never());

        StepVerifier.withVirtualTime(() -> renewer(manager).validAccessToken(exchange(), PRINCIPAL))
                .expectSubscription()
                .expectNoEvent(RENEWAL_TIMEOUT.minusSeconds(1))
                .thenAwait(Duration.ofSeconds(1))
                .expectError(TokenRenewalUnavailableException.class)
                .verify();
    }

    @Test
    void after_a_timeout_the_single_flight_entry_is_released_and_a_new_request_exchanges_again() {
        FakeSessionManager manager = new FakeSessionManager(expiredClient(),
                attempt -> attempt == 1 ? Mono.never() : Mono.just(rotated(attempt)));
        BffSessionTokenRenewer renewer = renewer(manager);
        MockServerWebExchange exchange = exchange();

        StepVerifier.withVirtualTime(() -> renewer.validAccessToken(exchange, PRINCIPAL))
                .expectSubscription()
                .thenAwait(RENEWAL_TIMEOUT)
                .expectError(TokenRenewalUnavailableException.class)
                .verify();
        assertThat(inFlightEntries(renewer)).isZero();

        StepVerifier.create(renewer.validAccessToken(exchange, PRINCIPAL))
                .expectNext("access-2")
                .verifyComplete();
        assertThat(manager.exchanges.get()).isEqualTo(2);
    }

    // ---- single-flight ----

    @Test
    void simultaneous_requests_of_one_session_share_a_single_exchange_and_the_same_token() {
        FakeSessionManager manager = new FakeSessionManager(expiredClient(),
                attempt -> Mono.delay(Duration.ofMillis(100)).thenReturn(rotated(attempt)));
        BffSessionTokenRenewer renewer = renewer(manager);
        MockServerWebExchange exchange = exchange();

        StepVerifier.withVirtualTime(() -> Flux.merge(Collections.nCopies(5, 0).stream()
                        .map(i -> renewer.validAccessToken(exchange, PRINCIPAL)).toList()).collectList())
                .expectSubscription()
                .thenAwait(Duration.ofMillis(100))
                .assertNext(tokens -> assertThat(tokens).hasSize(5).containsOnly("access-1"))
                .verifyComplete();

        assertThat(manager.exchanges.get()).isEqualTo(1);
        assertThat(inFlightEntries(renewer)).isZero();
    }

    @Test
    void a_new_renewal_after_the_previous_one_finished_creates_a_new_exchange() {
        FakeSessionManager manager = new FakeSessionManager(expiredClient(), attempt -> Mono.just(rotated(attempt)));
        BffSessionTokenRenewer renewer = renewer(manager);
        MockServerWebExchange exchange = exchange();

        StepVerifier.create(renewer.validAccessToken(exchange, PRINCIPAL))
                .expectNext("access-1")
                .verifyComplete();
        manager.session.set(expiredClient());
        StepVerifier.create(renewer.validAccessToken(exchange, PRINCIPAL))
                .expectNext("access-2")
                .verifyComplete();

        assertThat(manager.exchanges.get()).isEqualTo(2);
    }

    @Test
    void the_error_of_the_first_exchange_reaches_every_waiter_and_does_not_leave_the_entry_stuck() {
        FakeSessionManager manager = new FakeSessionManager(expiredClient(),
                attempt -> attempt == 1
                        ? Mono.delay(Duration.ofMillis(100)).then(Mono.<OAuth2AuthorizedClient>error(new IllegalStateException("keycloak down")))
                        : Mono.just(rotated(attempt)));
        BffSessionTokenRenewer renewer = renewer(manager);
        MockServerWebExchange exchange = exchange();

        StepVerifier.withVirtualTime(() -> Flux.merge(Collections.nCopies(3, 0).stream()
                        .map(i -> renewer.validAccessToken(exchange, PRINCIPAL)
                                .map(token -> "ok:" + token)
                                .onErrorResume(error -> Mono.just("error:" + error.getClass().getSimpleName())))
                        .toList()).collectList())
                .expectSubscription()
                .thenAwait(Duration.ofMillis(100))
                .assertNext(results -> assertThat(results).hasSize(3).containsOnly("error:TokenRenewalUnavailableException"))
                .verifyComplete();
        assertThat(manager.exchanges.get()).isEqualTo(1);
        assertThat(inFlightEntries(renewer)).isZero();

        StepVerifier.create(renewer.validAccessToken(exchange, PRINCIPAL))
                .expectNext("access-2")
                .verifyComplete();
        assertThat(manager.exchanges.get()).isEqualTo(2);
    }

    @Test
    void cancelling_the_first_subscriber_does_not_cancel_the_exchange_and_the_rotation_is_saved() {
        Sinks.One<OAuth2AuthorizedClient> keycloakAnswer = Sinks.one();
        FakeSessionManager manager = new FakeSessionManager(expiredClient(), attempt -> keycloakAnswer.asMono());
        BffSessionTokenRenewer renewer = renewer(manager);
        MockServerWebExchange exchange = exchange();

        StepVerifier.create(renewer.validAccessToken(exchange, PRINCIPAL))
                .expectSubscription()
                .thenCancel()
                .verify();
        assertThat(manager.exchanges.get()).isEqualTo(1);

        keycloakAnswer.tryEmitValue(rotated(1));

        assertThat(manager.session.get().getRefreshToken().getTokenValue()).isEqualTo("refresh-1");
        StepVerifier.create(renewer.validAccessToken(exchange, PRINCIPAL))
                .expectNext("access-1")
                .verifyComplete();
        assertThat(manager.exchanges.get()).isEqualTo(1);
        assertThat(inFlightEntries(renewer)).isZero();
    }

    // ---- el mapa no crece ----

    @Test
    void the_in_flight_map_is_empty_after_renewing_many_sessions() {
        FakeSessionManager manager = new FakeSessionManager(expiredClient(), attempt -> Mono.just(rotated(attempt)));
        BffSessionTokenRenewer renewer = renewer(manager);

        for (int i = 0; i < 20; i++) {
            manager.session.set(expiredClient());
            StepVerifier.create(renewer.validAccessToken(exchange(), PRINCIPAL))
                    .expectNextCount(1)
                    .verifyComplete();
        }

        assertThat(manager.exchanges.get()).isEqualTo(20);
        assertThat(inFlightEntries(renewer)).isZero();
    }

    @Test
    void the_in_flight_map_is_empty_after_failed_renewals_of_many_sessions() {
        FakeSessionManager manager = new FakeSessionManager(expiredClient(),
                attempt -> Mono.error(new IllegalStateException("keycloak down")));
        BffSessionTokenRenewer renewer = renewer(manager);

        for (int i = 0; i < 20; i++) {
            StepVerifier.create(renewer.validAccessToken(exchange(), PRINCIPAL))
                    .expectError(TokenRenewalUnavailableException.class)
                    .verify();
        }

        assertThat(inFlightEntries(renewer)).isZero();
    }

    // ---- ayudas ----

    private static void assertExpired(Throwable managerFailure) {
        FakeSessionManager manager = new FakeSessionManager(expiredClient(), attempt -> Mono.error(managerFailure));
        BffSessionTokenRenewer renewer = renewer(manager);

        StepVerifier.create(renewer.validAccessToken(exchange(), PRINCIPAL))
                .expectError(BffSessionExpiredException.class)
                .verify();

        assertThat(inFlightEntries(renewer)).isZero();
    }

    private static void assertUnavailable(Throwable managerFailure) {
        FakeSessionManager manager = new FakeSessionManager(expiredClient(), attempt -> Mono.error(managerFailure));
        BffSessionTokenRenewer renewer = renewer(manager);

        StepVerifier.create(renewer.validAccessToken(exchange(), PRINCIPAL))
                .expectError(TokenRenewalUnavailableException.class)
                .verify();

        assertThat(inFlightEntries(renewer)).isZero();
    }

    private static BffSessionTokenRenewer renewer(ReactiveOAuth2AuthorizedClientManager manager) {
        return new BffSessionTokenRenewer(manager, FIXED_TIME, RENEWAL_TIMEOUT);
    }

    private static MockServerWebExchange exchange() {
        return MockServerWebExchange.from(MockServerHttpRequest.get("/internal/v1/bff-session-token").build());
    }

    private static ClientAuthorizationException authorizationError(String code) {
        return new ClientAuthorizationException(new OAuth2Error(code), "keycloak");
    }

    private static OAuth2AuthorizedClient expiredClient() {
        return client("access-expired", "refresh-expired", NOW.minusSeconds(10));
    }

    private static OAuth2AuthorizedClient rotated(int attempt) {
        return client("access-" + attempt, "refresh-" + attempt, NOW.plusSeconds(300));
    }

    private static OAuth2AuthorizedClient client(String accessToken, String refreshToken, Instant expiresAt) {
        Instant issuedAt = expiresAt.minusSeconds(300);
        return new OAuth2AuthorizedClient(registration(), "keycloak-subject",
                new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, accessToken, issuedAt, expiresAt),
                new OAuth2RefreshToken(refreshToken, issuedAt));
    }

    private static ClientRegistration registration() {
        return ClientRegistration.withRegistrationId("keycloak")
                .clientId("security-baseline-bff").clientSecret("secret")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("http://localhost/callback")
                .authorizationUri("http://localhost/authorize").tokenUri("http://localhost/token")
                .build();
    }

    /** Suma las entradas de todo campo {@link Map} del renovador: el contrato es que no retiene nada al terminar. */
    private static int inFlightEntries(BffSessionTokenRenewer renewer) {
        int total = 0;
        try {
            for (Field field : BffSessionTokenRenewer.class.getDeclaredFields()) {
                if (Map.class.isAssignableFrom(field.getType())) {
                    field.setAccessible(true);
                    total += ((Map<?, ?>) field.get(renewer)).size();
                }
            }
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        }
        return total;
    }

    /**
     * Doble del manager de Spring: con el cliente de la sesion vigente lo devuelve tal cual; si esta vencido
     * cuenta un canje, delega en {@code renewal} (numero de canje -> resultado) y guarda la rotacion.
     */
    private static final class FakeSessionManager implements ReactiveOAuth2AuthorizedClientManager {
        final AtomicReference<OAuth2AuthorizedClient> session;
        final AtomicInteger exchanges = new AtomicInteger();
        final AtomicReference<OAuth2AuthorizeRequest> lastRequest = new AtomicReference<>();
        private final IntFunction<Mono<OAuth2AuthorizedClient>> renewal;

        FakeSessionManager(OAuth2AuthorizedClient initial, IntFunction<Mono<OAuth2AuthorizedClient>> renewal) {
            this.session = new AtomicReference<>(initial);
            this.renewal = renewal;
        }

        @Override
        public Mono<OAuth2AuthorizedClient> authorize(OAuth2AuthorizeRequest request) {
            lastRequest.set(request);
            return Mono.defer(() -> {
                OAuth2AuthorizedClient current = session.get();
                if (current != null && current.getAccessToken().getExpiresAt().isAfter(NOW)) {
                    return Mono.just(current);
                }
                int attempt = exchanges.incrementAndGet();
                return renewal.apply(attempt).doOnNext(session::set);
            });
        }
    }
}
