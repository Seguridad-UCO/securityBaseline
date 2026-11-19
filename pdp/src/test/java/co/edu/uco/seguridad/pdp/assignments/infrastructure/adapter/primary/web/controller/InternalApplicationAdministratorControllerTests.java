package co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.primary.web.controller;

import co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.primary.web.dto.request.raw.AssignApplicationAdministratorRawRequest;
import co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.primary.web.dto.response.AssignmentWebResponse;
import co.edu.uco.seguridad.shared.web.CorrelationWebFilter;
import co.edu.uco.seguridad.shared.web.RequestContext;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class InternalApplicationAdministratorControllerTests {

    private static final AssignmentWebResponse EXPECTED = new AssignmentWebResponse(
            "assignment-1", "user-1", "universidad-uco", "app-1", "role-admin", "2026-09-13T00:00:00Z", null);

    @Test
    void assign_uses_the_application_id_from_the_route_and_replies_with_201() {
        List<AssignApplicationAdministratorRawRequest> received = new ArrayList<>();
        InternalApplicationAdministratorController controller = new InternalApplicationAdministratorController(raw -> {
            received.add(raw);
            return Mono.just(EXPECTED);
        });
        MockServerWebExchange exchange = exchange();

        var response = controller.assign("app-1",
                new AssignApplicationAdministratorRawRequest(null, "user-1"), exchange).block();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().data()).isEqualTo(EXPECTED);
        assertThat(received).hasSize(1);
        assertThat(received.getFirst().applicationId()).isEqualTo("app-1");
        assertThat(received.getFirst().userId()).isEqualTo("user-1");
    }

    private static MockServerWebExchange exchange() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/internal/v1/applications/app-1/administrators"));
        exchange.getAttributes().put(CorrelationWebFilter.CONTEXT_ATTRIBUTE, new RequestContext("req-1", "corr-1"));
        return exchange;
    }
}
