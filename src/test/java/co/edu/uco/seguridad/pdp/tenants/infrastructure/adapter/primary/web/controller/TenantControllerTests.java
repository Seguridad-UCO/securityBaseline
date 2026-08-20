package co.edu.uco.seguridad.pdp.tenants.infrastructure.adapter.primary.web.controller;

import co.edu.uco.seguridad.pdp.commons.TenantId;
import co.edu.uco.seguridad.pdp.tenants.application.port.primary.dto.request.CreateTenantRequest;
import co.edu.uco.seguridad.pdp.tenants.application.port.primary.dto.response.TenantResponse;
import co.edu.uco.seguridad.pdp.tenants.domain.TenantName;
import co.edu.uco.seguridad.pdp.tenants.domain.TenantStatus;
import co.edu.uco.seguridad.pdp.tenants.infrastructure.adapter.primary.web.controller.TenantController.CreateTenantRawRequest;
import co.edu.uco.seguridad.shared.web.CorrelationWebFilter;
import co.edu.uco.seguridad.shared.web.RequestContext;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TenantControllerTests {

    @Test
    void create_delegates_the_raw_request_as_a_typed_request() {
        TenantResponse expected = new TenantResponse(new TenantId("universidad-uco"),
                new TenantName("Universidad UCO"), TenantStatus.ACTIVE);
        TenantController controller = new TenantController(
                dto -> Mono.just(expected), () -> Mono.just(List.of()));
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.post("/api/v1/tenants").build());
        exchange.getAttributes().put(CorrelationWebFilter.CONTEXT_ATTRIBUTE, new RequestContext("req-1", "corr-1"));

        Object response = controller.create(new CreateTenantRawRequest("universidad-uco", "Universidad UCO"), exchange)
                .block();

        assertThat(response).isNotNull();
    }

    @Test
    void list_returns_the_catalog_from_the_use_case() {
        TenantResponse tenant = new TenantResponse(new TenantId("universidad-uco"),
                new TenantName("Universidad UCO"), TenantStatus.ACTIVE);
        TenantController controller = new TenantController(
                (CreateTenantRequest dto) -> Mono.empty(), () -> Mono.just(List.of(tenant)));
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/tenants").build());
        exchange.getAttributes().put(CorrelationWebFilter.CONTEXT_ATTRIBUTE, new RequestContext("req-1", "corr-1"));

        Object response = controller.list(exchange).block();

        assertThat(response).isNotNull();
    }
}
