package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.controller;

import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.request.raw.AssignApplicationAdministratorRawRequest;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.request.raw.RemoveApplicationAdministratorRawRequest;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.response.ApplicationAdministratorWebResponse;
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

/** HU-020: rutas y códigos del autoservicio de administradores. */
class ApplicationAdministratorControllerTests {

    private static final ApplicationAdministratorWebResponse EXPECTED =
            new ApplicationAdministratorWebResponse("user-1", "2026-09-15T00:00:00Z", null);

    @Test
    void assign_delegates_to_the_interactor_and_replies_with_201() {
        ApplicationAdministratorController controller = new ApplicationAdministratorController(
                raw -> Mono.just(EXPECTED), raw -> Mono.empty(), raw -> Mono.just(List.of(EXPECTED)));
        MockServerWebExchange exchange =
                exchange(MockServerHttpRequest.post("/api/v1/applications/app-1/administrators"));

        var response = controller
                .assign("app-1", new AssignApplicationAdministratorRawRequest(null, "user-1"), exchange).block();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().data()).isEqualTo(EXPECTED);
    }

    @Test
    void remove_uses_the_application_and_user_from_the_route_and_replies_with_200() {
        List<RemoveApplicationAdministratorRawRequest> received = new ArrayList<>();
        ApplicationAdministratorController controller = new ApplicationAdministratorController(raw -> Mono.empty(), raw -> {
            received.add(raw);
            return Mono.empty();
        }, raw -> Mono.just(List.of(EXPECTED)));
        MockServerWebExchange exchange =
                exchange(MockServerHttpRequest.delete("/api/v1/applications/app-1/administrators/user-1"));

        var response = controller.remove("app-1", "user-1", exchange).block();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(received).hasSize(1);
        assertThat(received.getFirst().applicationId()).isEqualTo("app-1");
        assertThat(received.getFirst().userId()).isEqualTo("user-1");
    }

    @Test
    void list_delegates_to_the_interactor_and_replies_with_200() {
        ApplicationAdministratorController controller = new ApplicationAdministratorController(raw -> Mono.empty(),
                raw -> Mono.empty(), raw -> Mono.just(List.of(EXPECTED)));
        MockServerWebExchange exchange = exchange(MockServerHttpRequest.get("/api/v1/applications/app-1/administrators"));

        var response = controller.list("app-1", exchange).block();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().data()).containsExactly(EXPECTED);
    }

    private static MockServerWebExchange exchange(MockServerHttpRequest.BaseBuilder<?> request) {
        MockServerWebExchange exchange = MockServerWebExchange.from(request.build());
        exchange.getAttributes().put(CorrelationWebFilter.CONTEXT_ATTRIBUTE, new RequestContext("req-1", "corr-1"));
        return exchange;
    }
}
