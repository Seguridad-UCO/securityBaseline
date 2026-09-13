package co.edu.uco.seguridad.pdp.profiles.infrastructure.adapter.primary.web.controller;

import co.edu.uco.seguridad.pdp.profiles.infrastructure.adapter.primary.web.dto.request.raw.AddRoleToProfileRawRequest;
import co.edu.uco.seguridad.pdp.profiles.infrastructure.adapter.primary.web.dto.request.raw.DefineProfileRawRequest;
import co.edu.uco.seguridad.pdp.profiles.infrastructure.adapter.primary.web.dto.response.ProfileWebResponse;
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

/** Espejo de RoleControllerTests. */
class ProfileControllerTests {

    private static final ProfileWebResponse EXPECTED = new ProfileWebResponse(
            "profile-1", "Coordinador académico", "TENANT", "universidad-uco", null, List.of(), "2026-09-12T00:00:00Z");

    @Test
    void define_delegates_to_the_interactor_and_replies_with_201() {
        ProfileController controller = new ProfileController(
                raw -> Mono.just(EXPECTED), raw -> Mono.empty(), raw -> Mono.empty());
        MockServerWebExchange exchange = exchange();

        var response = controller.define(new DefineProfileRawRequest("Coordinador académico", "TENANT", null), exchange)
                .block();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().data()).isEqualTo(EXPECTED);
    }

    @Test
    void add_role_uses_the_profile_id_from_the_route_not_the_body() {
        List<AddRoleToProfileRawRequest> received = new ArrayList<>();
        ProfileController controller = new ProfileController(raw -> Mono.empty(), raw -> {
            received.add(raw);
            return Mono.just(EXPECTED);
        }, raw -> Mono.empty());
        MockServerWebExchange exchange = exchange();

        var response = controller.addRole("profile-from-route",
                new AddRoleToProfileRawRequest("profile-from-body", "role-1"), exchange).block();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(received).hasSize(1);
        assertThat(received.get(0).profileId()).isEqualTo("profile-from-route");
        assertThat(received.get(0).roleId()).isEqualTo("role-1");
    }

    @Test
    void list_delegates_to_the_interactor_and_replies_with_200() {
        PageResponse<ProfileWebResponse> page = new PageResponse<>(List.of(EXPECTED), 1L, 0, 0, 20);
        ProfileController controller = new ProfileController(raw -> Mono.empty(), raw -> Mono.empty(), raw -> Mono.just(page));
        MockServerWebExchange exchange = exchange();

        var response = controller.list(null, null, null, null, exchange).block();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().data().content()).containsExactly(EXPECTED);
    }

    private static MockServerWebExchange exchange() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.post("/api/v1/profiles"));
        exchange.getAttributes().put(CorrelationWebFilter.CONTEXT_ATTRIBUTE, new RequestContext("req-1", "corr-1"));
        return exchange;
    }
}
