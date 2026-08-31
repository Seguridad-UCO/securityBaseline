package co.edu.uco.seguridad.pdp.tenants.application.secondaryport.repository;

import co.edu.uco.seguridad.pdp.commons.TenantId;
import co.edu.uco.seguridad.pdp.tenants.domain.Tenant;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Puerto secundario para almacenamiento de inquilinos. Vacío significa "no registrado"; el llamador decide qué
 * implica eso.
 */
public interface TenantRepository {

    Mono<Tenant> findById(TenantId tenantId);

    Mono<Boolean> existsById(TenantId tenantId);

    Mono<Tenant> save(Tenant tenant);

    Flux<Tenant> findAll();
}
