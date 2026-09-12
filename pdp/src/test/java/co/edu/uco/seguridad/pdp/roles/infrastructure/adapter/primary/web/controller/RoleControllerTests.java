package co.edu.uco.seguridad.pdp.roles.infrastructure.adapter.primary.web.controller;

import co.edu.uco.seguridad.pdp.roles.infrastructure.adapter.primary.web.dto.request.raw.DefineRoleRawRequest;
import co.edu.uco.seguridad.pdp.roles.infrastructure.adapter.primary.web.dto.request.raw.GrantResourceRawRequest;
import co.edu.uco.seguridad.pdp.roles.infrastructure.adapter.primary.web.dto.response.RoleWebResponse;
import co.edu.uco.seguridad.shared.web.CorrelationWebFilter;
import co.edu.uco.seguridad.shared.web.PageResponse;
import co.edu.uco.seguridad.shared.web.RequestContext;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RoleControllerTests {

    private static final RoleWebResponse EXPECTED = new RoleWebResponse(
            "role-1", "Docente", "TENANT", "universidad-uco", null, List.of(), "2026-09-11T00:00:00Z");

    @Test
    void define_delegates_to_the_interactor_and_replies_with_201() {
        RoleController controller = new RoleController(
                raw -> Mono.just(EXPECTED), raw -> Mono.empty(), raw -> Mono.empty());
        MockServerWebExchange exchange = exchange();

        var response = controller.define(new DefineRoleRawRequest("Docente", "TENANT", null), exchange).block();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().data()).isEqualTo(EXPECTED);
    }

    @Test
    void grant_resource_uses_the_role_id_from_the_route_not_the_body() {
        List<GrantResourceRawRequest> received = new ArrayList<>();
        RoleController controller = new RoleController(raw -> Mono.empty(), raw -> {
            received.add(raw);
            return Mono.just(EXPECTED);
        }, raw -> Mono.empty());
        MockServerWebExchange exchange = exchange();

        var response = controller.grantResource("role-from-route",
                new GrantResourceRawRequest("role-from-body", "resource-1"), exchange).block();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(received).hasSize(1);
        assertThat(received.get(0).roleId()).isEqualTo("role-from-route");
        assertThat(received.get(0).resourceId()).isEqualTo("resource-1");
    }

    @Test
    void list_delegates_to_the_interactor_and_replies_with_200() {
        PageResponse<RoleWebResponse> page = new PageResponse<>(List.of(EXPECTED), 1L, 0, 0, 20);
        RoleController controller = new RoleController(raw -> Mono.empty(), raw -> Mono.empty(), raw -> Mono.just(page));
        MockServerWebExchange exchange = exchange();

        var response = controller.list(null, null, null, null, exchange).block();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().data().content()).containsExactly(EXPECTED);
    }

    private static MockServerWebExchange exchange() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.post("/api/v1/roles"));
        exchange.getAttributes().put(CorrelationWebFilter.CONTEXT_ATTRIBUTE, new RequestContext("req-1", "corr-1"));
        return exchange;
    }
}
