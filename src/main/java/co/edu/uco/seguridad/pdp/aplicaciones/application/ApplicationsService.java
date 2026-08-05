package co.edu.uco.seguridad.pdp.aplicaciones.application;

import co.edu.uco.seguridad.pdp.aplicaciones.*;
import co.edu.uco.seguridad.pdp.aplicaciones.domain.*;
import co.edu.uco.seguridad.pdp.commons.*;
import co.edu.uco.seguridad.pdp.tenants.TenantModuleApi;
import reactor.core.publisher.Mono;
import java.time.Instant;
import java.util.UUID;

public final class ApplicationsService implements ApplicationsModuleApi {
    private final TenantModuleApi tenants; private final ApplicationStore store;
    public ApplicationsService(TenantModuleApi tenants, ApplicationStore store) { this.tenants = tenants; this.store = store; }
    public Mono<RegisteredApplication> register(RegisterApplicationCommand command) {
        var name = new ApplicationName(command.name());
        return tenants.requireActive(command.tenantId()).then(store.exists(command.tenantId(), name))
            .flatMap(exists -> exists ? Mono.<Application>error(new DuplicateApplicationException(command.tenantId(), name.value())) : Mono.just(new Application(new ApplicationId(UUID.randomUUID()), command.tenantId(), name, Instant.now())))
            .flatMap(store::save).map(app -> new RegisteredApplication(app.id(), app.tenantId(), app.name().value(), app.registeredAt()));
    }
    public Mono<Void> remove(ApplicationId id) { return store.remove(id); }
    public interface ApplicationStore { Mono<Boolean> exists(TenantId tenant, ApplicationName name); Mono<Application> save(Application application); Mono<Void> remove(ApplicationId id); }
}
