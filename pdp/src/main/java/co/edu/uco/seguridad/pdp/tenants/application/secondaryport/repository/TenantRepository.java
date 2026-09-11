package co.edu.uco.seguridad.pdp.tenants.application.secondaryport.repository;

import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.tenants.domain.Tenant;
import co.edu.uco.seguridad.pdp.tenants.domain.model.TenantStatus;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Puerto secundario para almacenamiento de inquilinos. Vacío significa "no registrado"; el llamador decide qué
 * implica eso.
 *
 * <p>{@code findStatusById} existe en lugar de un {@code findById} genérico porque el único
 * consumidor —la validación de inquilino activo— solo necesita el estado: cargar el agregado
 * completo para leer un enum es traer de más.</p>
 */
public interface TenantRepository {

    Mono<TenantStatus> findStatusById(TenantId tenantId);

    Mono<Boolean> existsById(TenantId tenantId);

    Mono<Tenant> save(Tenant tenant);

    Flux<Tenant> findAll();
}
