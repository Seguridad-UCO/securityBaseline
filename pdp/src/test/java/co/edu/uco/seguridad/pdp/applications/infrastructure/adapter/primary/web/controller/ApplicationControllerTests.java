package co.edu.uco.seguridad.pdp.applications.infrastructure.adapter.primary.web.controller;

import co.edu.uco.seguridad.pdp.applications.infrastructure.adapter.primary.web.dto.request.raw.ListApplicationsRawRequest;
import co.edu.uco.seguridad.pdp.applications.infrastructure.adapter.primary.web.dto.request.raw.RegisterApplicationRawRequest;
import co.edu.uco.seguridad.pdp.applications.infrastructure.adapter.primary.web.dto.response.ApplicationRegisteredWebResponse;
import co.edu.uco.seguridad.pdp.applications.infrastructure.adapter.primary.web.dto.response.ApplicationWebResponse;
import co.edu.uco.seguridad.shared.web.CorrelationWebFilter;
import co.edu.uco.seguridad.shared.web.PageResponse;
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

class ApplicationControllerTests {

    private static final PageResponse<ApplicationWebResponse> EMPTY_PAGE =
            new PageResponse<>(List.of(), 0L, 0, 0, 20);

    @Test
    void register_delegates_to_the_interactor_and_replies_with_201() {
        ApplicationRegisteredWebResponse expected = new ApplicationRegisteredWebResponse("app-1", "universidad-uco",
                "gestion-academica", "", "https://example.com", "secreto-en-claro", Instant.now());
        ApplicationController controller = new ApplicationController(
                raw -> Mono.just(expected), query -> Mono.just(EMPTY_PAGE));
        MockServerWebExchange exchange = exchange(MockServerHttpRequest.post("/api/v1/applications"));

        var response = controller.register(
                new RegisterApplicationRawRequest("gestion-academica", "", "https://example.com"), exchange).block();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().data()).isEqualTo(expected);
    }

    @Test
    void list_delegates_to_the_interactor_and_replies_with_200() {
        ApplicationWebResponse app = new ApplicationWebResponse("app-1", "universidad-uco",
                "gestion-academica", "", "https://example.com", Instant.now());
        PageResponse<ApplicationWebResponse> page = new PageResponse<>(List.of(app), 1L, 0, 0, 20);
        ApplicationController controller = new ApplicationController(
                raw -> Mono.empty(), query -> Mono.just(page));
        MockServerWebExchange exchange = exchange(MockServerHttpRequest.get("/api/v1/applications"));

        var response = controller.list(null, null, null, null, null, exchange).block();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().data().content()).containsExactly(app);
        assertThat(response.getBody().data().total()).isEqualTo(1L);
    }

    @Test
    void list_passes_every_query_parameter_to_the_interactor_untouched() {
        List<ListApplicationsRawRequest> received = new ArrayList<>();
        ApplicationController controller = new ApplicationController(raw -> Mono.empty(), query -> {
            received.add(query);
            return Mono.just(EMPTY_PAGE);
        });
        MockServerWebExchange exchange = exchange(MockServerHttpRequest.get("/api/v1/applications"));

        controller.list("portal", "1", "5", null, null, exchange).block();

        assertThat(received).containsExactly(new ListApplicationsRawRequest("portal", "1", "5", null, null));
    }

    private static MockServerWebExchange exchange(MockServerHttpRequest.BaseBuilder<?> request) {
        MockServerWebExchange exchange = MockServerWebExchange.from(request.build());
        exchange.getAttributes().put(CorrelationWebFilter.CONTEXT_ATTRIBUTE, new RequestContext("req-1", "corr-1"));
        return exchange;
    }
}
