package co.edu.uco.seguridad.pdp.roles.infrastructure.adapter.primary.web.controller;

import co.edu.uco.seguridad.pdp.roles.infrastructure.adapter.primary.web.dto.response.RoleWebResponse;
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
 * HU-016: {@code define}/{@code grantResource} se movieron a {@code RoleAdministrationController}
 * (módulo {@code authorization}) — sus pruebas viven ahora en
 * {@code RoleAdministrationControllerTests}. Este controller solo conserva la consulta.
 */
class RoleControllerTests {

    private static final RoleWebResponse EXPECTED = new RoleWebResponse(
            "role-1", "Docente", "TENANT", "universidad-uco", null, List.of(), "2026-09-11T00:00:00Z");

    @Test
    void list_delegates_to_the_interactor_and_replies_with_200() {
        PageResponse<RoleWebResponse> page = new PageResponse<>(List.of(EXPECTED), 1L, 0, 0, 20);
        RoleController controller = new RoleController(raw -> Mono.just(page));
        MockServerWebExchange exchange = exchange();

        var response = controller.list(null, null, null, null, exchange).block();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().data().content()).containsExactly(EXPECTED);
    }

    private static MockServerWebExchange exchange() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/roles"));
        exchange.getAttributes().put(CorrelationWebFilter.CONTEXT_ATTRIBUTE, new RequestContext("req-1", "corr-1"));
        return exchange;
    }
}
