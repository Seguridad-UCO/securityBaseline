package co.edu.uco.seguridad.pdp.applications.infrastructure.adapter.primary.web.controller;

import co.edu.uco.seguridad.pdp.applications.infrastructure.adapter.primary.web.dto.request.raw.RegisterApplicationRawRequest;
import co.edu.uco.seguridad.pdp.applications.infrastructure.adapter.primary.web.dto.response.ApplicationWebResponse;
import co.edu.uco.seguridad.shared.web.CorrelationWebFilter;
import co.edu.uco.seguridad.shared.web.RequestContext;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ApplicationControllerTests {

    @Test
    void register_delegates_to_the_interactor_and_replies_with_201() {
        ApplicationWebResponse expected = new ApplicationWebResponse("app-1", "universidad-uco",
                "gestion-academica", "", "https://example.com", Instant.now());
        ApplicationController controller = new ApplicationController(
                raw -> Mono.just(expected), () -> Mono.just(List.of()));
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.post("/api/v1/applications").build());
        exchange.getAttributes().put(CorrelationWebFilter.CONTEXT_ATTRIBUTE, new RequestContext("req-1", "corr-1"));

        var response = controller.register(
                new RegisterApplicationRawRequest("gestion-academica", "", "https://example.com"), exchange).block();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().data()).isEqualTo(expected);
    }

    @Test
    void list_delegates_to_the_interactor_and_replies_with_200() {
        ApplicationWebResponse app = new ApplicationWebResponse("app-1", "universidad-uco",
                "gestion-academica", "", "https://example.com", Instant.now());
        ApplicationController controller = new ApplicationController(
                raw -> Mono.empty(), () -> Mono.just(List.of(app)));
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/applications").build());
        exchange.getAttributes().put(CorrelationWebFilter.CONTEXT_ATTRIBUTE, new RequestContext("req-1", "corr-1"));

        var response = controller.list(exchange).block();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().data()).containsExactly(app);
    }
}
