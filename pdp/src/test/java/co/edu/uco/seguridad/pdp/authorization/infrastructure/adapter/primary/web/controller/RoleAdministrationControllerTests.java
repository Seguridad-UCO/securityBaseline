package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.controller;

import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.request.raw.DefineRoleRawRequest;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.request.raw.GrantResourceRawRequest;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.response.RoleAdministrationWebResponse;
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

/** HU-016: mismas rutas y códigos que el antiguo {@code RoleController.define/grantResource}. */
class RoleAdministrationControllerTests {

    private static final RoleAdministrationWebResponse EXPECTED = new RoleAdministrationWebResponse(
            "role-1", "Docente", "APPLICATION", "universidad-uco", "app-1", List.of(), "2026-09-14T00:00:00Z");

    @Test
    void define_delegates_to_the_interactor_and_replies_with_201() {
        RoleAdministrationController controller = new RoleAdministrationController(
                raw -> Mono.just(EXPECTED), raw -> Mono.empty());
        MockServerWebExchange exchange = exchange(MockServerHttpRequest.post("/api/v1/roles"));

        var response = controller.define(new DefineRoleRawRequest("Docente", "APPLICATION", "app-1"), exchange).block();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().data()).isEqualTo(EXPECTED);
    }

    @Test
    void grant_resource_uses_the_role_id_from_the_route_not_the_body() {
        List<GrantResourceRawRequest> received = new ArrayList<>();
        RoleAdministrationController controller = new RoleAdministrationController(raw -> Mono.empty(), raw -> {
            received.add(raw);
            return Mono.just(EXPECTED);
        });
        MockServerWebExchange exchange = exchange(MockServerHttpRequest.post("/api/v1/roles/role-from-route/resources"));

        var response = controller.grantResource("role-from-route",
                new GrantResourceRawRequest("role-from-body", "resource-1"), exchange).block();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(received).hasSize(1);
        assertThat(received.getFirst().roleId()).isEqualTo("role-from-route");
        assertThat(received.getFirst().resourceId()).isEqualTo("resource-1");
    }

    private static MockServerWebExchange exchange(MockServerHttpRequest.BaseBuilder<?> request) {
        MockServerWebExchange exchange = MockServerWebExchange.from(request.build());
        exchange.getAttributes().put(CorrelationWebFilter.CONTEXT_ATTRIBUTE, new RequestContext("req-1", "corr-1"));
        return exchange;
    }
}
