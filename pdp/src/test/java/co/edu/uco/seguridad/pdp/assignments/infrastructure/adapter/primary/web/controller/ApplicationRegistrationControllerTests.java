package co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.primary.web.controller;

import co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.primary.web.dto.request.raw.RegisterApplicationWithFirstAdministratorRawRequest;
import co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.primary.web.dto.response.ApplicationRegisteredWebResponse;
import co.edu.uco.seguridad.shared.web.CorrelationWebFilter;
import co.edu.uco.seguridad.shared.web.RequestContext;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class ApplicationRegistrationControllerTests {

    @Test
    void register_delegates_to_the_interactor_and_replies_with_201() {
        ApplicationRegisteredWebResponse expected = new ApplicationRegisteredWebResponse("app-1", "universidad-uco",
                "gestion-academica", "", "https://example.com", "secreto-en-claro", Instant.now());
        ApplicationRegistrationController controller = new ApplicationRegistrationController(raw -> Mono.just(expected));
        MockServerWebExchange exchange = exchange();

        var response = controller.register(
                new RegisterApplicationWithFirstAdministratorRawRequest("gestion-academica", "", "https://example.com"),
                exchange).block();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().data()).isEqualTo(expected);
    }

    private static MockServerWebExchange exchange() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.post("/api/v1/applications"));
        exchange.getAttributes().put(CorrelationWebFilter.CONTEXT_ATTRIBUTE, new RequestContext("req-1", "corr-1"));
        return exchange;
    }
}
