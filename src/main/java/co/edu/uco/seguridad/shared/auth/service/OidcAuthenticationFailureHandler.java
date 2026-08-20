package co.edu.uco.seguridad.shared.auth.service;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.security.web.server.authentication.ServerAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.net.URI;

@Component
@Profile("keycloak")
public final class OidcAuthenticationFailureHandler implements ServerAuthenticationFailureHandler {

    private final OidcFlowStateService flowState;
    private final OidcRedirectPolicy redirectPolicy;

    OidcAuthenticationFailureHandler(OidcFlowStateService flowState, OidcRedirectPolicy redirectPolicy) {
        this.flowState = flowState;
        this.redirectPolicy = redirectPolicy;
    }

    @Override
    public Mono<Void> onAuthenticationFailure(WebFilterExchange exchange, AuthenticationException exception) {
        return flowState.clear(exchange.getExchange())
                .then(Mono.defer(() -> {
                    exchange.getExchange().getResponse().setStatusCode(HttpStatus.FOUND);
                    exchange.getExchange().getResponse().getHeaders()
                            .setLocation(URI.create(redirectPolicy.frontendAuthenticationError()));
                    return exchange.getExchange().getResponse().setComplete();
                }));
    }
}
