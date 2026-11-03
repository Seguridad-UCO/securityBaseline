package co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.primary.web.controller;

import co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.primary.web.dto.response.AssignmentWebResponse;
import co.edu.uco.seguridad.shared.web.CorrelationWebFilter;
import co.edu.uco.seguridad.shared.web.PageResponse;
import co.edu.uco.seguridad.shared.web.RequestContext;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * HU-018: assign/revoke se movieron a AssignmentAdministrationControllerTests (authorization).
 */
class AssignmentControllerTests {

    private static final AssignmentWebResponse EXPECTED = new AssignmentWebResponse(
            "assignment-1", "user-1", "universidad-uco", "app-1", "role-1", "2026-09-12T00:00:00Z", null);

    @Test
    void list_delegates_to_the_interactor_and_replies_with_200() {
        PageResponse<AssignmentWebResponse> page = new PageResponse<>(List.of(EXPECTED), 1L, 0, 0, 20);
        AssignmentController controller = new AssignmentController(raw -> Mono.just(page));
        MockServerWebExchange exchange = exchange();

        var response = controller.list("role-1", null, null, null, null, exchange).block();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().data().content()).containsExactly(EXPECTED);
    }

    private static MockServerWebExchange exchange() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/roles/role-1/assignments"));
        exchange.getAttributes().put(CorrelationWebFilter.CONTEXT_ATTRIBUTE, new RequestContext("req-1", "corr-1"));
        return exchange;
    }
}
