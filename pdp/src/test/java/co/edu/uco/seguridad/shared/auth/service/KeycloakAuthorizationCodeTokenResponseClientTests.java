package co.edu.uco.seguridad.shared.auth.service;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.endpoint.ReactiveOAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class KeycloakAuthorizationCodeTokenResponseClientTests {

    @Test
    void records_the_expiration_from_keycloak_and_returns_the_original_response() {
        var response = response(Map.of("refresh_expires_in", 1800));
        var tracker = new KeycloakRefreshTokenExpirationTracker(Instant::now);
        ReactiveOAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> delegate = mock();
        when(delegate.getTokenResponse(any())).thenReturn(Mono.just(response));
        var client = new KeycloakAuthorizationCodeTokenResponseClient(delegate, tracker);

        OAuth2AccessTokenResponse actual = client.getTokenResponse(mock()).block();

        assertThat(actual).isSameAs(response);
        assertThat(tracker.consumeExpiration("refresh-token"))
                .isEqualTo(response.getRefreshToken().getIssuedAt().plusSeconds(1800));
    }

    @Test
    void rejects_a_refresh_token_without_a_provider_expiration() {
        ReactiveOAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> delegate = mock();
        when(delegate.getTokenResponse(any())).thenReturn(Mono.just(response(Map.of())));
        var client = new KeycloakAuthorizationCodeTokenResponseClient(delegate,
                new KeycloakRefreshTokenExpirationTracker(Instant::now));

        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> client.getTokenResponse(mock()).block())
                .withMessage("Keycloak did not provide refresh_expires_in.");
    }

    private static OAuth2AccessTokenResponse response(Map<String, Object> additionalParameters) {
        return OAuth2AccessTokenResponse.withToken("access-token")
                .tokenType(OAuth2AccessToken.TokenType.BEARER)
                .expiresIn(300)
                .refreshToken("refresh-token")
                .additionalParameters(additionalParameters)
                .build();
    }
}
