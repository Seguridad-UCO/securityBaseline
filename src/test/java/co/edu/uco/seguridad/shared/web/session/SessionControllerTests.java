package co.edu.uco.seguridad.shared.web.session;

import co.edu.uco.seguridad.pdp.commons.TenantId;
import co.edu.uco.seguridad.shared.security.LocalUserPrincipal;
import co.edu.uco.seguridad.shared.web.CorrelationWebFilter;
import co.edu.uco.seguridad.shared.web.RequestContext;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SessionControllerTests {
    @Test
    void exposes_only_the_local_session_identity_to_the_spa() {
        var user = new LocalUserPrincipal("user-1", "google-sub", new TenantId("universidad-uco"), "david@uco.edu", "David Alzate");
        var authentication = new UsernamePasswordAuthenticationToken(user, null, List.of());
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/session").build());
        exchange.getAttributes().put(CorrelationWebFilter.CONTEXT_ATTRIBUTE, new RequestContext("request-1", "correlation-1"));

        var response = new SessionController().current(exchange)
                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication)).block();

        assertThat(response.data().subject()).isEqualTo("google-sub");
        assertThat(response.data().name()).isEqualTo("David Alzate");
        assertThat(response.data().email()).isEqualTo("david@uco.edu");
        assertThat(response.data().tenantId()).isEqualTo("universidad-uco");
    }
}
