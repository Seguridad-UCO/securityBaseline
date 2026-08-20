package co.edu.uco.seguridad.pdp.applications.infrastructure.adapter.primary.web.controller;

import co.edu.uco.seguridad.pdp.applications.application.port.primary.dto.response.RegisteredApplicationResponse;
import co.edu.uco.seguridad.pdp.applications.domain.ApplicationBaseUrl;
import co.edu.uco.seguridad.pdp.applications.infrastructure.adapter.primary.web.controller.ApplicationController.RegisterApplicationRawRequest;
import co.edu.uco.seguridad.pdp.commons.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.ApplicationName;
import co.edu.uco.seguridad.pdp.commons.TenantId;
import co.edu.uco.seguridad.shared.security.LocalUserPrincipal;
import co.edu.uco.seguridad.shared.web.CorrelationWebFilter;
import co.edu.uco.seguridad.shared.web.RequestContext;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ApplicationControllerTests {

    private static final TenantId TENANT = new TenantId("universidad-uco");

    @Test
    void register_derives_the_tenant_from_the_authenticated_session_not_the_body() {
        RegisteredApplicationResponse expected = new RegisteredApplicationResponse(
                new ApplicationId(UUID.randomUUID()), TENANT, new ApplicationName("gestion-academica"), "",
                new ApplicationBaseUrl("https://example.com"), Instant.now());
        ApplicationController controller = new ApplicationController(
                dto -> {
                    assertThat(dto.tenantId()).isEqualTo(TENANT);
                    return Mono.just(expected);
                },
                tenantId -> Flux.empty());
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.post("/api/v1/applications").build());
        exchange.getAttributes().put(CorrelationWebFilter.CONTEXT_ATTRIBUTE, new RequestContext("req-1", "corr-1"));

        Object response = authenticated(controller.register(
                new RegisterApplicationRawRequest("gestion-academica", "", "https://example.com"), exchange));

        assertThat(response).isNotNull();
    }

    @Test
    void list_scopes_the_query_to_the_authenticated_tenant() {
        ApplicationController controller = new ApplicationController(
                dto -> Mono.empty(),
                tenantId -> {
                    assertThat(tenantId).isEqualTo(TENANT);
                    return Flux.empty();
                });
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/applications").build());
        exchange.getAttributes().put(CorrelationWebFilter.CONTEXT_ATTRIBUTE, new RequestContext("req-1", "corr-1"));

        Object response = authenticated(controller.list(exchange));

        assertThat(response).isNotNull();
    }

    private static Object authenticated(Mono<?> call) {
        LocalUserPrincipal principal = new LocalUserPrincipal("user-1", "subject-1", TENANT, "test@uco.edu", "Test");
        var authentication = new UsernamePasswordAuthenticationToken(principal, null, List.of());
        return call.contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication)).block();
    }
}
