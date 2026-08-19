package co.edu.uco.seguridad.pdp.platform.infrastructure;

import co.edu.uco.seguridad.pdp.commons.TenantId;
import co.edu.uco.seguridad.pdp.platform.application.PlatformAdministrationService;
import co.edu.uco.seguridad.shared.security.LocalUserPrincipal;
import co.edu.uco.seguridad.shared.web.CorrelationWebFilter;
import co.edu.uco.seguridad.shared.web.RequestContext;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PlatformControllerTests {
    @Test
    void exposes_catalog_operations_using_the_authenticated_users_tenant() {
        PlatformAdministrationService service = mock(PlatformAdministrationService.class);
        var application = new PlatformAdministrationService.ApplicationView("app-1", "Horarios", "Gestión", "https://horarios.uco.edu", "universidad-uco", "now");
        var resource = new PlatformAdministrationService.ResourceView("resource-1", "app-1", "/empleados", "GET", "now");
        var tenant = new PlatformAdministrationService.TenantView("estudiantes-uco", "Estudiantes UCO", "ACTIVE");
        var user = new PlatformAdministrationService.UserView("user-1", "david@uco.edu", "David Alzate", "google", "estudiantes-uco", "now", "now");
        when(service.applications(any())).thenReturn(Mono.just(List.of(application)));
        when(service.createApplication(any(), any(), any(), any())).thenReturn(Mono.just(application));
        when(service.resources(any(), any())).thenReturn(Mono.just(List.of(resource)));
        when(service.createResource(any(), any(), any(), any())).thenReturn(Mono.just(resource));
        when(service.tenants()).thenReturn(Mono.just(List.of(tenant)));
        when(service.createTenant(any(), any())).thenReturn(Mono.just(tenant));
        when(service.users()).thenReturn(Mono.just(List.of(user)));
        when(service.assignTenant(any(), any())).thenReturn(Mono.just(user));

        var controller = new PlatformController(service);
        var exchange = exchange();
        var authentication = new UsernamePasswordAuthenticationToken(
                new LocalUserPrincipal("user-1", "google-subject", new TenantId("universidad-uco"), "david@uco.edu", "David Alzate"), null, List.of());

        assertThat(withIdentity(controller.applications(exchange), authentication)).isNotNull();
        assertThat(withIdentity(controller.app(new PlatformController.ApplicationRequest("Horarios", "Gestión", "https://horarios.uco.edu"), exchange), authentication)).isNotNull();
        assertThat(withIdentity(controller.resources("app-1", exchange), authentication)).isNotNull();
        assertThat(withIdentity(controller.resource("app-1", new PlatformController.ResourceRequest("/empleados", "GET"), exchange), authentication)).isNotNull();
        assertThat(controller.tenants(exchange).block()).isNotNull();
        assertThat(controller.tenant(new PlatformController.TenantRequest("estudiantes-uco", "Estudiantes UCO"), exchange).block()).isNotNull();
        assertThat(controller.users(exchange).block()).isNotNull();
        assertThat(withIdentity(controller.assign("user-1", new PlatformController.TenantAssignmentRequest("estudiantes-uco"), exchange), authentication)).isNotNull();
        Object savedContext = exchange.getSession().block().getAttribute("SPRING_SECURITY_CONTEXT");
        assertThat(savedContext).isNotNull();
    }

    private static Object withIdentity(Mono<?> operation, UsernamePasswordAuthenticationToken authentication) {
        return operation.contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication)).block();
    }

    private static MockServerWebExchange exchange() {
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1").build());
        exchange.getAttributes().put(CorrelationWebFilter.CONTEXT_ATTRIBUTE, new RequestContext("request-1", "correlation-1"));
        return exchange;
    }
}
