package co.edu.uco.seguridad.shared.auth.service;

import co.edu.uco.seguridad.shared.auth.model.TokenRefreshResult;
import co.edu.uco.seguridad.shared.auth.port.RefreshTokenStorePort;
import co.edu.uco.seguridad.shared.port.TimeProvider;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class KeycloakTokenRefreshAdapterTests {

    private static final Instant FIXED_NOW = Instant.parse("2026-10-21T16:00:00Z");
    @Test
    void exchanges_refresh_token_and_rotates_server_side_credential() {
        var storedToken = new AtomicReference<String>("previous-refresh-token");
        var rotatedToken = new AtomicReference<String>();
        var storedExpiry = new AtomicReference<Instant>();
        RefreshTokenStorePort store = refreshTokenStore(storedToken, rotatedToken, storedExpiry);
        var adapter = adapter(store, exchange -> Mono.just(tokenResponse(HttpStatus.OK,
                "{\"access_token\":\"new-access-token\",\"expires_in\":300,\"token_type\":\"Bearer\","
                        + "\"refresh_token\":\"rotated-refresh-token\",\"refresh_expires_in\":600}")));

        StepVerifier.create(adapter.refreshToken("keycloak-session-1"))
                .assertNext(result -> {
                    assertThat(result).isEqualTo(new TokenRefreshResult("new-access-token", 300, "Bearer"));
                    assertThat(result.accessToken()).isEqualTo("new-access-token");
                    assertThat(result.expiresIn()).isEqualTo(300);
                    assertThat(result.tokenType()).isEqualTo("Bearer");
                })
                .verifyComplete();

        assertThat(rotatedToken.get()).isEqualTo("rotated-refresh-token");
        assertThat(storedToken.get()).isEqualTo("rotated-refresh-token");
        assertThat(storedExpiry.get()).isEqualTo(FIXED_NOW.plusSeconds(600));
    }

    @Test
    void reports_expired_session_for_keycloak_invalid_grant() {
        RefreshTokenStorePort store = refreshTokenStore(new AtomicReference<>("refresh-token"),
                new AtomicReference<>(), new AtomicReference<>());
        var adapter = adapter(store, exchange -> Mono.just(tokenResponse(HttpStatus.BAD_REQUEST,
                "{\"error\":\"invalid_grant\"}")));

        StepVerifier.create(adapter.refreshToken("keycloak-session-1"))
                .expectErrorMatches(error -> error instanceof ResponseStatusException rse
                        && rse.getStatusCode().equals(HttpStatus.UNAUTHORIZED)
                        && "SESSION_EXPIRED".equals(rse.getReason()))
                .verify();
    }

    @Test
    void reports_service_unavailable_when_keycloak_is_unavailable() {
        RefreshTokenStorePort store = refreshTokenStore(new AtomicReference<>("refresh-token"),
                new AtomicReference<>(), new AtomicReference<>());
        var adapter = adapter(store, exchange -> Mono.just(tokenResponse(HttpStatus.SERVICE_UNAVAILABLE, "{}")));

        StepVerifier.create(adapter.refreshToken("keycloak-session-1"))
                .expectErrorMatches(error -> error instanceof ResponseStatusException rse
                        && rse.getStatusCode().equals(HttpStatus.SERVICE_UNAVAILABLE))
                .verify();
    }

    private static KeycloakTokenRefreshAdapter adapter(RefreshTokenStorePort store,
                                                       org.springframework.web.reactive.function.client.ExchangeFunction exchange) {
        ClientRegistration registration = ClientRegistration.withRegistrationId("keycloak")
                .clientId("security-baseline-bff")
                .clientSecret("test-client-secret")
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("http://localhost/callback")
                .authorizationUri("http://localhost/auth")
                .tokenUri("http://localhost/token")
                .jwkSetUri("http://localhost/jwks")
                .userInfoUri("http://localhost/userinfo")
                .userNameAttributeName("sub")
                .build();
        return new KeycloakTokenRefreshAdapter(store,
                new InMemoryReactiveClientRegistrationRepository(registration),
                WebClient.builder().exchangeFunction(exchange),
                (TimeProvider) () -> FIXED_NOW);
    }

    private static ClientResponse tokenResponse(HttpStatus status, String body) {
        return ClientResponse.create(status)
                .header("Content-Type", "application/json")
                .body(body)
                .build();
    }

    private static RefreshTokenStorePort refreshTokenStore(AtomicReference<String> storedToken,
                                                           AtomicReference<String> rotatedToken,
                                                           AtomicReference<Instant> storedExpiry) {
        return new RefreshTokenStorePort() {
            @Override
            public Mono<Void> store(String sessionId, String refreshToken, Instant expiresAt) {
                rotatedToken.set(refreshToken);
                storedToken.set(refreshToken);
                storedExpiry.set(expiresAt);
                return Mono.empty();
            }

            @Override
            public Mono<String> find(String sessionId) {
                return Mono.justOrEmpty(storedToken.get());
            }

            @Override
            public Mono<Void> delete(String sessionId) {
                storedToken.set(null);
                return Mono.empty();
            }
        };
    }
}
