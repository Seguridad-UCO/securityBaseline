package co.edu.uco.seguridad.shared.auth.service;

import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.endpoint.ReactiveOAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.WebClientReactiveAuthorizationCodeTokenResponseClient;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import reactor.core.publisher.Mono;

import java.util.Objects;

public final class KeycloakAuthorizationCodeTokenResponseClient
        implements ReactiveOAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> {

    private final ReactiveOAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> delegate;
    private final KeycloakRefreshTokenExpirationTracker expirationTracker;

    public KeycloakAuthorizationCodeTokenResponseClient(KeycloakRefreshTokenExpirationTracker expirationTracker) {
        this(new WebClientReactiveAuthorizationCodeTokenResponseClient(), expirationTracker);
    }

    KeycloakAuthorizationCodeTokenResponseClient(
            ReactiveOAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> delegate,
            KeycloakRefreshTokenExpirationTracker expirationTracker) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.expirationTracker = Objects.requireNonNull(expirationTracker, "expirationTracker");
    }

    @Override
    public Mono<OAuth2AccessTokenResponse> getTokenResponse(OAuth2AuthorizationCodeGrantRequest grantRequest) {
        return delegate.getTokenResponse(grantRequest)
                .doOnNext(expirationTracker::record);
    }
}
