package co.edu.uco.seguridad.pdp.resources.infrastructure.adapter.primary.web.controller;

import co.edu.uco.seguridad.pdp.commons.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.ResourceId;
import co.edu.uco.seguridad.pdp.commons.TenantId;
import co.edu.uco.seguridad.pdp.resources.application.port.primary.dto.response.RegisteredProtectedResourceResponse;
import co.edu.uco.seguridad.pdp.resources.domain.HttpVerb;
import co.edu.uco.seguridad.pdp.resources.domain.ResourcePath;
import co.edu.uco.seguridad.pdp.resources.infrastructure.adapter.primary.web.controller.ProtectedResourceController.RegisterProtectedResourceRawRequest;
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

class ProtectedResourceControllerTests {

    private static final TenantId TENANT = new TenantId("universidad-uco");

    @Test
    void register_derives_the_tenant_from_the_session_and_the_application_from_the_path() {
        ApplicationId applicationId = new ApplicationId(UUID.randomUUID());
        RegisteredProtectedResourceResponse expected = new RegisteredProtectedResourceResponse(
                new ResourceId(UUID.randomUUID()), applicationId, TENANT, new ResourcePath("/estudiantes"),
                HttpVerb.GET, Instant.now());
        ProtectedResourceController controller = new ProtectedResourceController(
                dto -> {
                    assertThat(dto.tenantId()).isEqualTo(TENANT);
                    assertThat(dto.applicationId()).isEqualTo(applicationId);
                    return Mono.just(expected);
                },
                appId -> Flux.empty());
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/v1/applications/" + applicationId.value() + "/resources").build());
        exchange.getAttributes().put(CorrelationWebFilter.CONTEXT_ATTRIBUTE, new RequestContext("req-1", "corr-1"));

        Object response = authenticated(controller.register(applicationId.value().toString(),
                new RegisterProtectedResourceRawRequest("/estudiantes", "GET"), exchange));

        assertThat(response).isNotNull();
    }

    @Test
    void list_scopes_the_query_to_the_application_in_the_path() {
        ApplicationId applicationId = new ApplicationId(UUID.randomUUID());
        ProtectedResourceController controller = new ProtectedResourceController(
                dto -> Mono.empty(),
                appId -> {
                    assertThat(appId).isEqualTo(applicationId);
                    return Flux.empty();
                });
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/applications/" + applicationId.value() + "/resources").build());
        exchange.getAttributes().put(CorrelationWebFilter.CONTEXT_ATTRIBUTE, new RequestContext("req-1", "corr-1"));

        Object response = controller.list(applicationId.value().toString(), exchange).block();

        assertThat(response).isNotNull();
    }

    private static Object authenticated(Mono<?> call) {
        LocalUserPrincipal principal = new LocalUserPrincipal("user-1", "subject-1", TENANT, "test@uco.edu", "Test");
        var authentication = new UsernamePasswordAuthenticationToken(principal, null, List.of());
        return call.contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication)).block();
    }
}
