package co.edu.uco.seguridad.pdp.recursos.application;

import co.edu.uco.seguridad.pdp.aplicaciones.*;
import co.edu.uco.seguridad.pdp.commons.*;
import co.edu.uco.seguridad.pdp.recursos.*;
import co.edu.uco.seguridad.pdp.recursos.domain.*;
import co.edu.uco.seguridad.pdp.tenants.TenantModuleApi;
import reactor.core.publisher.Mono;
import java.time.Instant;
import java.util.UUID;

/** E-1 orchestration. Recursos is allowed to depend on Tenants and Aplicaciones by the PDP map. */
public final class RegisterProtectedApplicationService implements RegisterProtectedApplicationUseCase {
    private final TenantModuleApi tenants; private final ApplicationsModuleApi applications; private final ResourceStore resources; private final AuditPort audit;
    public RegisterProtectedApplicationService(TenantModuleApi tenants, ApplicationsModuleApi applications, ResourceStore resources, AuditPort audit) { this.tenants = tenants; this.applications = applications; this.resources = resources; this.audit = audit; }
    public Mono<ProtectedApplicationCatalogEntry> register(RegisterProtectedApplicationCommand command) {
        TenantId tenant = new TenantId(command.tenantId()); ResourceCode resourceCode = new ResourceCode(command.resourceCode()); ActionCode action = new ActionCode(command.action());
        return tenants.requireActive(tenant).then(applications.register(new RegisterApplicationCommand(tenant, command.applicationName())))
            .flatMap(app -> resources.save(new ProtectedResource(new ResourceId(UUID.randomUUID()), app.id(), tenant, resourceCode, action, Instant.now()))
                .flatMap(resource -> audit.registered(app, resource).thenReturn(new ProtectedApplicationCatalogEntry(app.id(), resource.id(), tenant, app.name(), resource.code().value(), resource.action().value(), app.registeredAt()))
                    .onErrorResume(error -> resources.remove(resource.id()).then(applications.remove(app.id())).then(Mono.error(error)))));
    }
    public interface ResourceStore { Mono<ProtectedResource> save(ProtectedResource resource); Mono<Void> remove(ResourceId id); }
    public interface AuditPort { Mono<Void> registered(RegisteredApplication application, ProtectedResource resource); }
}
