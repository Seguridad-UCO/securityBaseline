package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.controller;

import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.request.raw.AssignRoleRawRequest;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.request.raw.RevokeAssignmentRawRequest;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.response.AssignmentAdministrationWebResponse;
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

/**
 * HU-018: mismas rutas y códigos que el antiguo {@code AssignmentController.assign/revoke}.
 */
class AssignmentAdministrationControllerTests {

    private static final AssignmentAdministrationWebResponse EXPECTED = new AssignmentAdministrationWebResponse(
            "assignment-1", "user-1", "universidad-uco", "app-1", "role-1", "2026-09-15T00:00:00Z", null);

    @Test
    void assign_delegates_to_the_interactor_and_replies_with_201() {
        AssignmentAdministrationController controller = new AssignmentAdministrationController(
                raw -> Mono.just(EXPECTED), raw -> Mono.empty());
        MockServerWebExchange exchange = exchange(MockServerHttpRequest.post("/api/v1/roles/role-1/assignments"));

        var response = controller.assign("role-1", new AssignRoleRawRequest(null, "user-1", "app-1"), exchange).block();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().data()).isEqualTo(EXPECTED);
    }

    @Test
    void revoke_uses_the_assignment_id_from_the_route_and_replies_with_200() {
        List<RevokeAssignmentRawRequest> received = new ArrayList<>();
        AssignmentAdministrationController controller = new AssignmentAdministrationController(raw -> Mono.empty(), raw -> {
            received.add(raw);
            return Mono.empty();
        });
        MockServerWebExchange exchange = exchange(MockServerHttpRequest.delete("/api/v1/roles/role-1/assignments/assignment-1"));

        var response = controller.revoke("role-1", "assignment-1", exchange).block();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(received).hasSize(1);
        assertThat(received.getFirst().assignmentId()).isEqualTo("assignment-1");
    }

    private static MockServerWebExchange exchange(MockServerHttpRequest.BaseBuilder<?> request) {
        MockServerWebExchange exchange = MockServerWebExchange.from(request.build());
        exchange.getAttributes().put(CorrelationWebFilter.CONTEXT_ATTRIBUTE, new RequestContext("req-1", "corr-1"));
        return exchange;
    }
}
