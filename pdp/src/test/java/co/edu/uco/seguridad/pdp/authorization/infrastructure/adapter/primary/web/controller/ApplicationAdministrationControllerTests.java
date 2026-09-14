package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.controller;

import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.request.raw.ApplicationAdministrationRawRequest;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.response.AdministeredApplicationWebResponse;
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

class ApplicationAdministrationControllerTests {

    @Test
    void remove_uses_the_application_id_from_the_route_and_replies_with_200_and_no_body() {
        List<ApplicationAdministrationRawRequest> received = new ArrayList<>();
        ApplicationAdministrationController controller = new ApplicationAdministrationController(raw -> {
            received.add(raw);
            return Mono.empty();
        }, raw -> Mono.empty());
        MockServerWebExchange exchange = exchange(MockServerHttpRequest.delete("/api/v1/applications/app-1"));

        var response = controller.remove("app-1", exchange).block();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().data()).isNull();
        assertThat(received).hasSize(1);
        assertThat(received.getFirst().applicationId()).isEqualTo("app-1");
    }

    @Test
    void rotate_uses_the_application_id_from_the_route_and_replies_with_201() {
        AdministeredApplicationWebResponse expected = new AdministeredApplicationWebResponse("app-1",
                "universidad-uco", "gestion-academica", "", "https://example.com", "secreto-nuevo-en-claro",
                Instant.now());
        List<ApplicationAdministrationRawRequest> received = new ArrayList<>();
        ApplicationAdministrationController controller = new ApplicationAdministrationController(raw -> Mono.empty(), raw -> {
            received.add(raw);
            return Mono.just(expected);
        });
        MockServerWebExchange exchange = exchange(
                MockServerHttpRequest.post("/api/v1/applications/app-1/credential-rotations"));

        var response = controller.rotate("app-1", exchange).block();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().data()).isEqualTo(expected);
        assertThat(received).hasSize(1);
        assertThat(received.getFirst().applicationId()).isEqualTo("app-1");
    }

    private static MockServerWebExchange exchange(MockServerHttpRequest.BaseBuilder<?> request) {
        MockServerWebExchange exchange = MockServerWebExchange.from(request.build());
        exchange.getAttributes().put(CorrelationWebFilter.CONTEXT_ATTRIBUTE, new RequestContext("req-1", "corr-1"));
        return exchange;
    }
}
