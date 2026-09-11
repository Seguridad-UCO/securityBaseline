package co.edu.uco.seguridad.shared.web.session;

import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.shared.security.LocalUserPrincipal;
import co.edu.uco.seguridad.shared.web.CorrelationWebFilter;
import co.edu.uco.seguridad.shared.web.RequestContext;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.web.server.csrf.DefaultCsrfToken;
import reactor.core.publisher.Mono;

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

        assertThat(response.code()).isEqualTo("SESSION_ACTIVE");
        assertThat(response.data().subject()).isEqualTo("google-sub");
        assertThat(response.data().name()).isEqualTo("David Alzate");
        assertThat(response.data().email()).isEqualTo("david@uco.edu");
        assertThat(response.data().tenantId()).isEqualTo("universidad-uco");
    }

    @Test
    void exposes_the_minimal_identity_for_a_bearer_jwt_and_bootstraps_the_csrf_token() {
        var authentication = new UsernamePasswordAuthenticationToken(
                co.edu.uco.seguridad.shared.security.TestJwtSupport.jwt("estudiantes-uco", "jwt-subject"), null, List.of());
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/session").build());
        exchange.getAttributes().put(CorrelationWebFilter.CONTEXT_ATTRIBUTE, new RequestContext("request-2", "correlation-2"));
        exchange.getAttributes().put(org.springframework.security.web.server.csrf.CsrfToken.class.getName(),
                Mono.just(new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "csrf-value")));

        var response = new SessionController().current(exchange)
                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication)).block();

        assertThat(response.code()).isEqualTo("SESSION_ACTIVE");
        assertThat(response.data().subject()).isEqualTo("jwt-subject");
        assertThat(response.data().tenantId()).isEqualTo("estudiantes-uco");
        assertThat(response.data().name()).isEmpty();
        assertThat(response.data().email()).isEmpty();
    }
}
