package co.edu.uco.seguridad.pdp.identity.infrastructure.adapter.primary.web.controller;

import co.edu.uco.seguridad.pdp.commons.TenantId;
import co.edu.uco.seguridad.pdp.identity.application.port.primary.dto.response.UserResponse;
import co.edu.uco.seguridad.pdp.identity.domain.UserId;
import co.edu.uco.seguridad.pdp.identity.infrastructure.adapter.primary.web.controller.UserController.TenantAssignmentRawRequest;
import co.edu.uco.seguridad.shared.web.CorrelationWebFilter;
import co.edu.uco.seguridad.shared.web.RequestContext;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UserControllerTests {

    @Test
    void list_returns_the_catalog_from_the_use_case() {
        UserId id = new UserId(UUID.randomUUID());
        UserResponse user = new UserResponse(id, "david@uco.edu", "David", "google",
                new TenantId("universidad-uco"), Instant.now(), Instant.now());
        UserController controller = new UserController(() -> Mono.just(List.of(user)), dto -> Mono.empty());
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/users").build());
        exchange.getAttributes().put(CorrelationWebFilter.CONTEXT_ATTRIBUTE, new RequestContext("req-1", "corr-1"));

        Object response = controller.list(exchange).block();

        assertThat(response).isNotNull();
    }

    @Test
    void assign_delegates_the_new_tenant_code() {
        UserId id = new UserId(UUID.randomUUID());
        UserResponse updated = new UserResponse(id, "david@uco.edu", "David", "google",
                new TenantId("otra-universidad"), Instant.now(), Instant.now());
        UserController controller = new UserController(() -> Mono.empty(), dto -> {
            assertThat(dto.userId()).isEqualTo(id);
            assertThat(dto.tenantId()).isEqualTo(new TenantId("otra-universidad"));
            return Mono.just(updated);
        });
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.put("/api/v1/users/" + id.value() + "/tenant").build());
        exchange.getAttributes().put(CorrelationWebFilter.CONTEXT_ATTRIBUTE, new RequestContext("req-1", "corr-1"));

        Object response = controller.assign(id.value().toString(), new TenantAssignmentRawRequest("otra-universidad"),
                exchange).block();

        assertThat(response).isNotNull();
    }
}
