package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.controller;

import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.request.raw.AssignProfileRawRequest;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.request.raw.RevokeProfileAssignmentRawRequest;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.response.ProfileAssignmentAdministrationWebResponse;
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
 * HU-019: mismas rutas y códigos que el antiguo {@code ProfileAssignmentController.assign/revoke}.
 */
class ProfileAssignmentAdministrationControllerTests {

    private static final ProfileAssignmentAdministrationWebResponse EXPECTED = new ProfileAssignmentAdministrationWebResponse(
            "profile-assignment-1", "user-1", "universidad-uco", "app-1", "profile-1", List.of(),
            "2026-09-15T00:00:00Z", null);

    @Test
    void assign_delegates_to_the_interactor_and_replies_with_201() {
        ProfileAssignmentAdministrationController controller = new ProfileAssignmentAdministrationController(
                raw -> Mono.just(EXPECTED), raw -> Mono.empty());
        MockServerWebExchange exchange = exchange(MockServerHttpRequest.post("/api/v1/profiles/profile-1/assignments"));

        var response = controller.assign("profile-1", new AssignProfileRawRequest(null, "user-1", "app-1"), exchange)
                .block();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().data()).isEqualTo(EXPECTED);
    }

    @Test
    void revoke_uses_the_profile_assignment_id_from_the_route_and_replies_with_200() {
        List<RevokeProfileAssignmentRawRequest> received = new ArrayList<>();
        ProfileAssignmentAdministrationController controller = new ProfileAssignmentAdministrationController(
                raw -> Mono.empty(), raw -> {
            received.add(raw);
            return Mono.empty();
        });
        MockServerWebExchange exchange = exchange(
                MockServerHttpRequest.delete("/api/v1/profiles/profile-1/assignments/profile-assignment-1"));

        var response = controller.revoke("profile-1", "profile-assignment-1", exchange).block();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(received).hasSize(1);
        assertThat(received.getFirst().profileAssignmentId()).isEqualTo("profile-assignment-1");
    }

    private static MockServerWebExchange exchange(MockServerHttpRequest.BaseBuilder<?> request) {
        MockServerWebExchange exchange = MockServerWebExchange.from(request.build());
        exchange.getAttributes().put(CorrelationWebFilter.CONTEXT_ATTRIBUTE, new RequestContext("req-1", "corr-1"));
        return exchange;
    }
}
