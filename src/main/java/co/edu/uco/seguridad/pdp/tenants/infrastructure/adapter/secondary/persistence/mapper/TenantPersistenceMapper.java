package co.edu.uco.seguridad.pdp.tenants.infrastructure.adapter.secondary.persistence.mapper;

import co.edu.uco.seguridad.pdp.commons.TenantId;
import co.edu.uco.seguridad.pdp.tenants.domain.Tenant;
import co.edu.uco.seguridad.pdp.tenants.domain.TenantStatus;
import co.edu.uco.seguridad.pdp.tenants.infrastructure.adapter.secondary.persistence.entity.TenantEntity;

/**
 * Traduce entre la fila de almacenamiento y la entidad de dominio. Solo formato: no toma decisiones que una
 * regla podría tomar.
 */
public final class TenantPersistenceMapper {

    private TenantPersistenceMapper() {
    }

    public static Tenant toDomain(TenantEntity entity) {
        return new Tenant(new TenantId(entity.getId()), TenantStatus.valueOf(entity.getStatus()));
    }

    public static TenantEntity toEntity(Tenant tenant) {
        return new TenantEntity(tenant.id().value(), tenant.status().name());
    }
}
