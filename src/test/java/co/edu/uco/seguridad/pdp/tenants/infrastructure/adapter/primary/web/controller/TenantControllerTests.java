package co.edu.uco.seguridad.pdp.tenants.infrastructure.adapter.primary.web.controller;

import co.edu.uco.seguridad.pdp.tenants.infrastructure.adapter.primary.web.dto.request.raw.CreateTenantRawRequest;
import co.edu.uco.seguridad.pdp.tenants.infrastructure.adapter.primary.web.dto.response.TenantWebResponse;
import co.edu.uco.seguridad.shared.web.CorrelationWebFilter;
import co.edu.uco.seguridad.shared.web.RequestContext;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TenantControllerTests {

    @Test
    void create_delegates_to_the_interactor_and_replies_with_201() {
        TenantWebResponse expected = new TenantWebResponse("universidad-uco", "Universidad UCO", "ACTIVE");
        TenantController controller = new TenantController(
                raw -> Mono.just(expected), () -> Mono.just(List.of()));
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.post("/api/v1/tenants").build());
        exchange.getAttributes().put(CorrelationWebFilter.CONTEXT_ATTRIBUTE, new RequestContext("req-1", "corr-1"));

        var response = controller.create(new CreateTenantRawRequest("universidad-uco", "Universidad UCO"), exchange)
                .block();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().data()).isEqualTo(expected);
    }

    @Test
    void list_delegates_to_the_interactor_and_replies_with_200() {
        TenantWebResponse tenant = new TenantWebResponse("universidad-uco", "Universidad UCO", "ACTIVE");
        TenantController controller = new TenantController(
                raw -> Mono.empty(), () -> Mono.just(List.of(tenant)));
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/tenants").build());
        exchange.getAttributes().put(CorrelationWebFilter.CONTEXT_ATTRIBUTE, new RequestContext("req-1", "corr-1"));

        var response = controller.list(exchange).block();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().data()).containsExactly(tenant);
    }
}
