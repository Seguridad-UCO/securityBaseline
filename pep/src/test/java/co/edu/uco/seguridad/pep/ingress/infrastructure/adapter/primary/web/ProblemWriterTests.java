package co.edu.uco.seguridad.pep.ingress.infrastructure.adapter.primary.web;

import co.edu.uco.seguridad.pep.commons.EnforcementFailure;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * HU-031: el 503 del canal BFF sugiere reintento; los demás 503 no cambian.
 */
class ProblemWriterTests {
    private final ProblemWriter problems = new ProblemWriter(new tools.jackson.databind.ObjectMapper());

    @Test
    void bff_session_unavailable_is_503_problem_json_with_retry_after_5() {
        MockServerWebExchange exchange = exchange();

        StepVerifier.create(problems.write(exchange, new EnforcementFailure(
                EnforcementFailure.Kind.UNAVAILABLE, "BFF_SESSION_UNAVAILABLE"))).verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(exchange.getResponse().getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(exchange.getResponse().getHeaders().getFirst(HttpHeaders.RETRY_AFTER)).isEqualTo("5");
        StepVerifier.create(exchange.getResponse().getBodyAsString())
                .assertNext(body -> assertThat(body).contains("\"code\":\"BFF_SESSION_UNAVAILABLE\"")
                        .contains("request-1").contains("correlation-1"))
                .verifyComplete();
    }

    @Test
    void pdp_unavailable_is_503_without_retry_after() {
        MockServerWebExchange exchange = exchange();

        StepVerifier.create(problems.write(exchange, new EnforcementFailure(
                EnforcementFailure.Kind.UNAVAILABLE, "PDP_UNAVAILABLE"))).verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(exchange.getResponse().getHeaders().containsHeader(HttpHeaders.RETRY_AFTER)).isFalse();
    }

    @Test
    void rate_limited_keeps_its_retry_after_of_1() {
        MockServerWebExchange exchange = exchange();

        StepVerifier.create(problems.write(exchange, new EnforcementFailure(
                EnforcementFailure.Kind.RATE_LIMITED, "RATE_LIMIT_EXCEEDED"))).verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(exchange.getResponse().getHeaders().getFirst(HttpHeaders.RETRY_AFTER)).isEqualTo("1");
    }

    private static MockServerWebExchange exchange() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/apps/demo/resource"));
        exchange.getAttributes().put("pep.requestId", "request-1");
        exchange.getAttributes().put("pep.correlationId", "correlation-1");
        return exchange;
    }
}
