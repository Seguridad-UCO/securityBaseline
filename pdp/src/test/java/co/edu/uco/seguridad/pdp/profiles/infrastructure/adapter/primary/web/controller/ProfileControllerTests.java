package co.edu.uco.seguridad.pdp.profiles.infrastructure.adapter.primary.web.controller;

import co.edu.uco.seguridad.pdp.profiles.infrastructure.adapter.primary.web.dto.response.ProfileWebResponse;
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

/** HU-019: define/addRole se movieron a ProfileAdministrationControllerTests (authorization). */
class ProfileControllerTests {

    private static final ProfileWebResponse EXPECTED = new ProfileWebResponse(
            "profile-1", "Coordinador académico", "TENANT", "universidad-uco", null, List.of(), "2026-09-12T00:00:00Z");

    @Test
    void list_delegates_to_the_interactor_and_replies_with_200() {
        PageResponse<ProfileWebResponse> page = new PageResponse<>(List.of(EXPECTED), 1L, 0, 0, 20);
        ProfileController controller = new ProfileController(raw -> Mono.just(page));
        MockServerWebExchange exchange = exchange();

        var response = controller.list(null, null, null, null, exchange).block();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().data().content()).containsExactly(EXPECTED);
    }

    private static MockServerWebExchange exchange() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/profiles"));
        exchange.getAttributes().put(CorrelationWebFilter.CONTEXT_ATTRIBUTE, new RequestContext("req-1", "corr-1"));
        return exchange;
    }
}
