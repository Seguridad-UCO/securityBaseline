package co.edu.uco.seguridad.pdp.resources.infrastructure.adapter.primary.web.controller;

import co.edu.uco.seguridad.pdp.resources.infrastructure.adapter.primary.web.dto.request.raw.RegisterApplicationWithInitialResourceRawRequest;
import co.edu.uco.seguridad.pdp.resources.infrastructure.adapter.primary.web.dto.response.ApplicationWithInitialResourceWebResponse;
import co.edu.uco.seguridad.shared.web.CorrelationWebFilter;
import co.edu.uco.seguridad.shared.web.RequestContext;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ApplicationWithInitialResourceControllerTests {

    @Test
    void register_delegates_to_the_interactor_and_replies_with_201() {
        ApplicationWithInitialResourceWebResponse expected = new ApplicationWithInitialResourceWebResponse(
                "app-1", "universidad-uco", "gestion-academica", "", "https://example.com",
                "secreto-en-claro", Instant.now(), "resource-1", "/estudiantes", "GET", Instant.now());
        List<RegisterApplicationWithInitialResourceRawRequest> received = new ArrayList<>();
        ApplicationWithInitialResourceController controller = new ApplicationWithInitialResourceController(raw -> {
            received.add(raw);
            return Mono.just(expected);
        });
        MockServerWebExchange exchange = exchange(MockServerHttpRequest.post("/api/v1/applications/with-initial-resource"));

        var response = controller.register(
                new RegisterApplicationWithInitialResourceRawRequest("gestion-academica", "", "https://example.com",
                        "/estudiantes", "GET"),
                exchange).block();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().data()).isEqualTo(expected);
        assertThat(received).containsExactly(
                new RegisterApplicationWithInitialResourceRawRequest("gestion-academica", "", "https://example.com",
                        "/estudiantes", "GET"));
    }

    private static MockServerWebExchange exchange(MockServerHttpRequest.BaseBuilder<?> request) {
        MockServerWebExchange exchange = MockServerWebExchange.from(request.build());
        exchange.getAttributes().put(CorrelationWebFilter.CONTEXT_ATTRIBUTE, new RequestContext("req-1", "corr-1"));
        return exchange;
    }
}
