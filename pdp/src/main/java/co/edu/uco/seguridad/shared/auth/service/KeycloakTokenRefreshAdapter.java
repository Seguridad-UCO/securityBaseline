package co.edu.uco.seguridad.shared.auth.service;

import co.edu.uco.seguridad.shared.auth.model.TokenRefreshResult;
import co.edu.uco.seguridad.shared.auth.port.RefreshTokenStorePort;
import co.edu.uco.seguridad.shared.auth.port.TokenRefreshPort;
import co.edu.uco.seguridad.shared.port.TimeProvider;
import org.springframework.context.annotation.Profile;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Objects;

@Service
@Profile("keycloak")
public final class KeycloakTokenRefreshAdapter implements TokenRefreshPort {

    private static final String KEYCLOAK_REGISTRATION_ID = "keycloak";
    private static final ParameterizedTypeReference<Map<String, Object>> TOKEN_RESPONSE_TYPE =
            new ParameterizedTypeReference<>() { };

    private final RefreshTokenStorePort refreshTokenStore;
    private final ReactiveClientRegistrationRepository clientRegistrations;
    private final WebClient webClient;
    private final TimeProvider timeProvider;

    public KeycloakTokenRefreshAdapter(RefreshTokenStorePort refreshTokenStore,
                                       ReactiveClientRegistrationRepository clientRegistrations,
                                       WebClient.Builder webClientBuilder,
                                       TimeProvider timeProvider) {
        this.refreshTokenStore = Objects.requireNonNull(refreshTokenStore, "refreshTokenStore");
        this.clientRegistrations = Objects.requireNonNull(clientRegistrations, "clientRegistrations");
        this.webClient = Objects.requireNonNull(webClientBuilder, "webClientBuilder").build();
        this.timeProvider = Objects.requireNonNull(timeProvider, "timeProvider");
    }

    @Override
    public Mono<TokenRefreshResult> refreshToken(String sessionId) {
        return refreshTokenStore.find(sessionId)
                .switchIfEmpty(Mono.error(sessionExpired()))
                .flatMap(refreshToken -> clientRegistrations.findByRegistrationId(KEYCLOAK_REGISTRATION_ID)
                        .switchIfEmpty(Mono.error(new ResponseStatusException(
                                HttpStatus.SERVICE_UNAVAILABLE, "Keycloak client registration is unavailable")))
                        .flatMap(client -> exchangeRefreshToken(client, sessionId, refreshToken)));
    }

    private Mono<TokenRefreshResult> exchangeRefreshToken(ClientRegistration client, String sessionId, String refreshToken) {
        return webClient.post()
                .uri(client.getProviderDetails().getTokenUri())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .headers(headers -> headers.setBasicAuth(client.getClientId(), client.getClientSecret()))
                .body(BodyInserters.fromFormData("grant_type", "refresh_token")
                        .with("refresh_token", refreshToken))
                .retrieve()
                .onStatus(status -> status.is4xxClientError(), response -> response
                        .bodyToMono(TOKEN_RESPONSE_TYPE)
                        .defaultIfEmpty(Map.of())
                        .map(body -> "invalid_grant".equals(body.get("error"))
                                ? sessionExpired()
                                : new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                                        "Keycloak rejected the token refresh")))
                .onStatus(status -> status.is5xxServerError(), response -> Mono.error(
                        new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Keycloak is unavailable")))
                .bodyToMono(TOKEN_RESPONSE_TYPE)
                .flatMap(body -> toRefreshResult(body, sessionId))
                .onErrorMap(WebClientRequestException.class, error ->
                        new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Keycloak is unavailable", error));
    }

    private Mono<TokenRefreshResult> toRefreshResult(Map<String, Object> body, String sessionId) {
        String accessToken = stringValue(body, "access_token");
        String tokenType = stringValue(body, "token_type");
        long expiresIn = positiveLong(body, "expires_in");
        String rotatedRefreshToken = stringValueOrNull(body, "refresh_token");
        if (rotatedRefreshToken == null) {
            return Mono.just(new TokenRefreshResult(accessToken, expiresIn, tokenType));
        }

        long refreshExpiresIn = positiveLong(body, "refresh_expires_in");
        return refreshTokenStore.store(sessionId, rotatedRefreshToken,
                        timeProvider.now().plusSeconds(refreshExpiresIn))
                .thenReturn(new TokenRefreshResult(accessToken, expiresIn, tokenType));
    }

    private static String stringValue(Map<String, Object> body, String key) {
        String value = stringValueOrNull(body, key);
        if (!StringUtils.hasText(value)) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Keycloak returned an incomplete token response");
        }
        return value;
    }

    private static String stringValueOrNull(Map<String, Object> body, String key) {
        Object value = body.get(key);
        return value instanceof String string ? string : null;
    }

    private static long positiveLong(Map<String, Object> body, String key) {
        Object value = body.get(key);
        if (!(value instanceof Number number) || number.longValue() <= 0) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Keycloak returned an incomplete token response");
        }
        return number.longValue();
    }

    private static ResponseStatusException sessionExpired() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "SESSION_EXPIRED");
    }
}
