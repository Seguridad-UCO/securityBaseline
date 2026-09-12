package co.edu.uco.seguridad.shared.auth.service;

import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;

class OidcAuthenticationFailureHandlerTests {

    @Test
    void redirects_authentication_failures_back_to_the_frontend_and_clears_the_flow_intent() {
        OidcAuthenticationFailureHandler handler = new OidcAuthenticationFailureHandler(new OidcFlowStateService(),
                new OidcRedirectPolicy("http://localhost:5173/login?from=oauth"));
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/login/oauth2/code/keycloak").build());
        exchange.getSession().block().getAttributes().put(OidcFlowStateService.FLOW_INTENT_ATTRIBUTE, "REGISTER");

        handler.onAuthenticationFailure(webFilterExchange(exchange), new BadCredentialsException("boom")).block();

        assertThat(exchange.getResponse().getStatusCode()).hasToString("302 FOUND");
        assertThat(String.valueOf(exchange.getResponse().getHeaders().getLocation())).isEqualTo("http://localhost:5173/login?error=authentication");
        assertThat((Object) exchange.getSession().block().getAttribute(OidcFlowStateService.FLOW_INTENT_ATTRIBUTE)).isNull();
    }

    private static WebFilterExchange webFilterExchange(MockServerWebExchange exchange) {
        WebFilterChain chain = currentExchange -> Mono.empty();
        return new WebFilterExchange(exchange, chain);
    }
}
