package co.edu.uco.seguridad.pdp.aplicaciones.infrastructure;

import co.edu.uco.seguridad.pdp.aplicaciones.*;
import co.edu.uco.seguridad.pdp.aplicaciones.application.ApplicationsService;
import co.edu.uco.seguridad.pdp.aplicaciones.domain.*;
import co.edu.uco.seguridad.pdp.commons.*;
import co.edu.uco.seguridad.pdp.tenants.TenantModuleApi;
import org.springframework.context.annotation.*;
import reactor.core.publisher.Mono;
import java.util.concurrent.ConcurrentHashMap;

@Configuration class ApplicationsConfiguration {
    @Bean ApplicationsService.ApplicationStore applicationStore() { return new InMemoryStore(); }
    @Bean ApplicationsModuleApi applicationsModuleApi(TenantModuleApi tenants, ApplicationsService.ApplicationStore store) { return new ApplicationsService(tenants, store); }
    private static final class InMemoryStore implements ApplicationsService.ApplicationStore {
        private final ConcurrentHashMap<ApplicationId, Application> data = new ConcurrentHashMap<>();
        public Mono<Boolean> exists(TenantId tenant, ApplicationName name) { return Mono.fromSupplier(() -> data.values().stream().anyMatch(a -> a.tenantId().equals(tenant) && a.name().value().equalsIgnoreCase(name.value()))); }
        public Mono<Application> save(Application application) { return Mono.fromSupplier(() -> { data.put(application.id(), application); return application; }); }
        public Mono<Void> remove(ApplicationId id) { return Mono.fromRunnable(() -> data.remove(id)); }
    }
}
