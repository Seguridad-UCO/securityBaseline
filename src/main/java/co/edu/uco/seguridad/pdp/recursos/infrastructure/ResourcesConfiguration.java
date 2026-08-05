package co.edu.uco.seguridad.pdp.recursos.infrastructure;

import co.edu.uco.seguridad.pdp.aplicaciones.*;
import co.edu.uco.seguridad.pdp.commons.ResourceId;
import co.edu.uco.seguridad.pdp.recursos.*;
import co.edu.uco.seguridad.pdp.recursos.application.RegisterProtectedApplicationService;
import co.edu.uco.seguridad.pdp.recursos.domain.ProtectedResource;
import co.edu.uco.seguridad.pdp.tenants.TenantModuleApi;
import org.springframework.context.annotation.*;
import reactor.core.publisher.Mono;
import java.util.concurrent.ConcurrentHashMap;

@Configuration class ResourcesConfiguration {
    @Bean RegisterProtectedApplicationService.ResourceStore resourceStore() { return new InMemoryResourceStore(); }
    @Bean RegisterProtectedApplicationService.AuditPort resourceAuditPort() { return (app, resource) -> Mono.empty(); }
    @Bean RegisterProtectedApplicationUseCase registerProtectedApplicationUseCase(TenantModuleApi tenants, ApplicationsModuleApi applications, RegisterProtectedApplicationService.ResourceStore store, RegisterProtectedApplicationService.AuditPort audit) { return new RegisterProtectedApplicationService(tenants, applications, store, audit); }
    private static final class InMemoryResourceStore implements RegisterProtectedApplicationService.ResourceStore {
        private final ConcurrentHashMap<ResourceId, ProtectedResource> data = new ConcurrentHashMap<>();
        public Mono<ProtectedResource> save(ProtectedResource resource) { return Mono.fromSupplier(() -> { data.put(resource.id(), resource); return resource; }); }
        public Mono<Void> remove(ResourceId id) { return Mono.fromRunnable(() -> data.remove(id)); }
    }
}
