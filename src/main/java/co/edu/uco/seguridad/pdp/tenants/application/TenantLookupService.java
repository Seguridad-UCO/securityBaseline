package co.edu.uco.seguridad.pdp.tenants.application;

import co.edu.uco.seguridad.pdp.commons.TenantId;
import co.edu.uco.seguridad.pdp.tenants.*;
import reactor.core.publisher.Mono;

public final class TenantLookupService implements TenantModuleApi {
    private final TenantStore store;
    public TenantLookupService(TenantStore store) { this.store = store; }
    public Mono<TenantSnapshot> requireActive(TenantId id) { return store.find(id).filter(TenantSnapshot::active).switchIfEmpty(Mono.error(new TenantUnavailableException(id))); }
    public interface TenantStore { Mono<TenantSnapshot> find(TenantId id); }
}
