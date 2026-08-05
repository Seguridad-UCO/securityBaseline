package co.edu.uco.seguridad.pdp.tenants.infrastructure;

import co.edu.uco.seguridad.pdp.commons.TenantId;
import co.edu.uco.seguridad.pdp.tenants.*;
import co.edu.uco.seguridad.pdp.tenants.application.TenantLookupService;
import org.springframework.context.annotation.*;
import reactor.core.publisher.Mono;
import java.util.Set;

@Configuration class TenantConfiguration {
    @Bean TenantLookupService.TenantStore tenantStore() { Set<String> active = Set.of("tenant-a", "universidad-uco"); return id -> Mono.justOrEmpty(active.contains(id.value()) ? new TenantSnapshot(id, true) : null); }
    @Bean TenantModuleApi tenantModuleApi(TenantLookupService.TenantStore store) { return new TenantLookupService(store); }
}
