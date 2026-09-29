package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.controller;

import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.request.raw.AddRoleToProfileRawRequest;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.request.raw.DefineProfileRawRequest;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.response.ProfileAdministrationWebResponse;
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
 * HU-019: mismas rutas y códigos que el antiguo {@code ProfileController.define/addRole}.
 */
class ProfileAdministrationControllerTests {

    private static final ProfileAdministrationWebResponse EXPECTED = new ProfileAdministrationWebResponse(
            "profile-1", "Docentes de matematicas", "APPLICATION", "universidad-uco", "app-1", List.of(),
            "2026-09-15T00:00:00Z");

    @Test
    void define_delegates_to_the_interactor_and_replies_with_201() {
        ProfileAdministrationController controller = new ProfileAdministrationController(
                raw -> Mono.just(EXPECTED), raw -> Mono.empty());
        MockServerWebExchange exchange = exchange(MockServerHttpRequest.post("/api/v1/profiles"));

        var response = controller.define(new DefineProfileRawRequest("Docentes de matematicas", "APPLICATION", "app-1"),
                exchange).block();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().data()).isEqualTo(EXPECTED);
    }

    @Test
    void add_role_uses_the_profile_id_from_the_route_not_the_body() {
        List<AddRoleToProfileRawRequest> received = new ArrayList<>();
        ProfileAdministrationController controller = new ProfileAdministrationController(raw -> Mono.empty(), raw -> {
            received.add(raw);
            return Mono.just(EXPECTED);
        });
        MockServerWebExchange exchange = exchange(MockServerHttpRequest.post("/api/v1/profiles/profile-from-route/roles"));

        var response = controller.addRole("profile-from-route",
                new AddRoleToProfileRawRequest("profile-from-body", "role-1"), exchange).block();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(received).hasSize(1);
        assertThat(received.getFirst().profileId()).isEqualTo("profile-from-route");
        assertThat(received.getFirst().roleId()).isEqualTo("role-1");
    }

    private static MockServerWebExchange exchange(MockServerHttpRequest.BaseBuilder<?> request) {
        MockServerWebExchange exchange = MockServerWebExchange.from(request.build());
        exchange.getAttributes().put(CorrelationWebFilter.CONTEXT_ATTRIBUTE, new RequestContext("req-1", "corr-1"));
        return exchange;
    }
}
