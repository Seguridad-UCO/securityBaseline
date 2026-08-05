package co.edu.uco.seguridad.pdp.tenants;

import co.edu.uco.seguridad.pdp.commons.TenantId;
import reactor.core.publisher.Mono;

/** Public API: other PDP modules may only know this contract, never tenant internals. */
public interface TenantModuleApi { Mono<TenantSnapshot> requireActive(TenantId tenantId); }
