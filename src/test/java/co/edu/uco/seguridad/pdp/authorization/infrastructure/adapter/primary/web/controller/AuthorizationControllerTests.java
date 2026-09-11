package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.controller;

import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.request.raw.AuthorizeRawRequest;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.response.AccessDecisionWebResponse;
import co.edu.uco.seguridad.shared.web.CorrelationWebFilter;
import co.edu.uco.seguridad.shared.web.RequestContext;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AuthorizationControllerTests {

    @Test
    void authorize_delegates_to_the_interactor_and_replies_with_200() {
        AccessDecisionWebResponse expected = new AccessDecisionWebResponse(UUID.randomUUID().toString(), "DENY",
                "NO_APPLICABLE_POLICY", List.of(), "req-1", "corr-1", "2026-09-06T00:00:00Z");
        AuthorizationController controller = new AuthorizationController(raw -> Mono.just(expected));
        MockServerWebExchange exchange =
                MockServerWebExchange.from(MockServerHttpRequest.post("/api/v1/authorize").build());
        exchange.getAttributes().put(CorrelationWebFilter.CONTEXT_ATTRIBUTE, new RequestContext("req-1", "corr-1"));

        var response = controller
                .authorize(new AuthorizeRawRequest(UUID.randomUUID().toString(), "/estudiantes", "GET"), exchange)
                .block();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().data()).isEqualTo(expected);
    }
}
