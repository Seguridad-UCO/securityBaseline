package co.edu.uco.seguridad.pdp.tenants.infrastructure.adapter.secondary.persistence.repository;

import co.edu.uco.seguridad.pdp.commons.TenantId;
import co.edu.uco.seguridad.pdp.tenants.application.port.secondary.repository.TenantRepository;
import co.edu.uco.seguridad.pdp.tenants.domain.Tenant;
import co.edu.uco.seguridad.pdp.tenants.infrastructure.adapter.secondary.persistence.entity.TenantEntity;
import co.edu.uco.seguridad.pdp.tenants.infrastructure.adapter.secondary.persistence.mapper.TenantPersistenceMapper;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Adaptador secundario (driven) ficticio sustituible (criterio 07). Almacena entidades, no objetos de
 * dominio, por lo que reemplazarlo con una base de datos real no cambia nada por encima de esta clase.
 */
public final class InMemoryTenantRepository implements TenantRepository {

    private final Map<String, TenantEntity> rows = new ConcurrentHashMap<>();

    public InMemoryTenantRepository(List<TenantEntity> seed) {
        seed.forEach(entity -> rows.put(entity.getId(), entity));
    }

    @Override
    public Mono<Tenant> findById(TenantId tenantId) {
        return Mono.fromSupplier(() -> rows.get(tenantId.value()))
                .map(TenantPersistenceMapper::toDomain);
    }
}
