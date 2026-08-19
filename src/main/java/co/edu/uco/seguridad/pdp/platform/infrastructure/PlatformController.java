package co.edu.uco.seguridad.pdp.platform.infrastructure;

import co.edu.uco.seguridad.pdp.platform.application.PlatformAdministrationService;
import co.edu.uco.seguridad.shared.security.LocalUserPrincipal;
import co.edu.uco.seguridad.shared.security.SecurityContext;
import co.edu.uco.seguridad.shared.web.ApiResponse;
import co.edu.uco.seguridad.shared.web.CorrelationWebFilter;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.server.context.WebSessionServerSecurityContextRepository;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/** Adaptador HTTP del catálogo. Los tenantIds se derivan de la sesión, nunca del formulario. */
@RestController
@RequestMapping("/api/v1")
final class PlatformController {
    private final PlatformAdministrationService service;
    private final WebSessionServerSecurityContextRepository sessions = new WebSessionServerSecurityContextRepository();
    PlatformController(PlatformAdministrationService service) { this.service = service; }
    record ApplicationRequest(String name,String description,String baseUrl) { }
    record ResourceRequest(String path,String method) { }
    record TenantRequest(String code,String name) { }
    record TenantAssignmentRequest(String tenantCode) { }
    @GetMapping("/applications") Mono<?> applications(ServerWebExchange x) { return SecurityContext.currentPrincipal().flatMap(p->service.applications(p.tenantId().value())).map(d->ApiResponse.success("APPLICATIONS_LISTED","Applications listed",d,CorrelationWebFilter.context(x))); }
    @PostMapping("/applications") Mono<?> app(@RequestBody ApplicationRequest r,ServerWebExchange x) { return SecurityContext.currentPrincipal().flatMap(p->service.createApplication(p.tenantId().value(),r.name(),r.description(),r.baseUrl())).map(d->ApiResponse.success("APPLICATION_CREATED","Application created",d,CorrelationWebFilter.context(x))); }
    @GetMapping("/applications/{id}/resources") Mono<?> resources(@PathVariable String id,ServerWebExchange x){return SecurityContext.currentPrincipal().flatMap(p->service.resources(p.tenantId().value(),id)).map(d->ApiResponse.success("RESOURCES_LISTED","Resources listed",d,CorrelationWebFilter.context(x)));}
    @PostMapping("/applications/{id}/resources") Mono<?> resource(@PathVariable String id,@RequestBody ResourceRequest r,ServerWebExchange x){return SecurityContext.currentPrincipal().flatMap(p->service.createResource(p.tenantId().value(),id,r.path(),r.method())).map(d->ApiResponse.success("RESOURCE_CREATED","Resource created",d,CorrelationWebFilter.context(x)));}
    @GetMapping("/tenants") Mono<?> tenants(ServerWebExchange x){return service.tenants().map(d->ApiResponse.success("TENANTS_LISTED","Tenants listed",d,CorrelationWebFilter.context(x)));}
    @PostMapping("/tenants") Mono<?> tenant(@RequestBody TenantRequest r,ServerWebExchange x){return service.createTenant(r.code(),r.name()).map(d->ApiResponse.success("TENANT_CREATED","Tenant created",d,CorrelationWebFilter.context(x)));}
    @GetMapping("/users") Mono<?> users(ServerWebExchange x){return service.users().map(d->ApiResponse.success("USERS_LISTED","Users listed",d,CorrelationWebFilter.context(x)));}
    @PutMapping("/users/{id}/tenant")
    Mono<?> assign(@PathVariable String id, @RequestBody TenantAssignmentRequest r, ServerWebExchange x) {
        return service.assignTenant(id, r.tenantCode())
                .flatMap(updated -> refreshCurrentSessionTenant(x, updated)
                        .thenReturn(ApiResponse.success("USER_TENANT_ASSIGNED", "Tenant assigned", updated,
                                CorrelationWebFilter.context(x))));
    }

    /** Reemite la sesión BFF solo si la reasignación corresponde al usuario que opera. */
    private Mono<Void> refreshCurrentSessionTenant(ServerWebExchange exchange,
            PlatformAdministrationService.UserView updated) {
        return ReactiveSecurityContextHolder.getContext()
                .map(context -> context.getAuthentication().getPrincipal())
                .ofType(LocalUserPrincipal.class)
                .filter(current -> current.userId().equals(updated.id()))
                .flatMap(current -> {
                    LocalUserPrincipal refreshed = new LocalUserPrincipal(current.userId(), current.subject(),
                            new co.edu.uco.seguridad.pdp.commons.TenantId(updated.tenantId()),
                            current.email(), current.name());
                    var authentication = new UsernamePasswordAuthenticationToken(refreshed, null, currentAuthorities());
                    return sessions.save(exchange, new SecurityContextImpl(authentication));
                })
                .then();
    }

    private static java.util.List<org.springframework.security.core.GrantedAuthority> currentAuthorities() {
        return java.util.List.of();
    }
}
