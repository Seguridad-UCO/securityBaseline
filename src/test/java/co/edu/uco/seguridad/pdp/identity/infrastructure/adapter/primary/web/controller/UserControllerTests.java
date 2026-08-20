package co.edu.uco.seguridad.pdp.identity.infrastructure.adapter.primary.web.controller;

import co.edu.uco.seguridad.pdp.identity.infrastructure.adapter.primary.web.dto.request.raw.AssignTenantBodyRequest;
import co.edu.uco.seguridad.pdp.identity.infrastructure.adapter.primary.web.dto.response.UserWebResponse;
import co.edu.uco.seguridad.shared.web.CorrelationWebFilter;
import co.edu.uco.seguridad.shared.web.RequestContext;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UserControllerTests {

    @Test
    void list_delegates_to_the_interactor_and_replies_with_200() {
        UserWebResponse user = new UserWebResponse(UUID.randomUUID().toString(), "david@uco.edu", "David", "google",
                "universidad-uco", Instant.now(), Instant.now());
        UserController controller = new UserController(() -> Mono.just(List.of(user)), raw -> Mono.empty());
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/users").build());
        exchange.getAttributes().put(CorrelationWebFilter.CONTEXT_ATTRIBUTE, new RequestContext("req-1", "corr-1"));

        var response = controller.list(exchange).block();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().data()).containsExactly(user);
    }

    @Test
    void assign_combines_the_path_id_with_the_body_and_replies_with_200() {
        String id = UUID.randomUUID().toString();
        UserWebResponse updated = new UserWebResponse(id, "david@uco.edu", "David", "google",
                "otra-universidad", Instant.now(), Instant.now());
        UserController controller = new UserController(() -> Mono.empty(), raw -> {
            assertThat(raw.userId()).isEqualTo(id);
            assertThat(raw.tenantCode()).isEqualTo("otra-universidad");
            return Mono.just(updated);
        });
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.put("/api/v1/users/" + id + "/tenant").build());
        exchange.getAttributes().put(CorrelationWebFilter.CONTEXT_ATTRIBUTE, new RequestContext("req-1", "corr-1"));

        var response = controller.assign(id, new AssignTenantBodyRequest("otra-universidad"), exchange).block();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().data()).isEqualTo(updated);
    }
}
