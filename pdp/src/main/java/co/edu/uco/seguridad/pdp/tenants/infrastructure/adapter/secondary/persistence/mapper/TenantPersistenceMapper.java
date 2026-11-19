package co.edu.uco.seguridad.pdp.tenants.infrastructure.adapter.secondary.persistence.mapper;

import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.tenants.domain.Tenant;
import co.edu.uco.seguridad.pdp.tenants.domain.model.TenantName;
import co.edu.uco.seguridad.pdp.tenants.domain.model.TenantStatus;
import co.edu.uco.seguridad.pdp.tenants.infrastructure.adapter.secondary.persistence.entity.TenantEntity;

/**
 * Traduce entre la fila de almacenamiento y la entidad de dominio. Solo formato: no toma decisiones que una
 * regla podría tomar.
 */
public final class TenantPersistenceMapper {

    private TenantPersistenceMapper() {
    }

    public static Tenant toDomain(TenantEntity entity) {
        return new Tenant(new TenantId(entity.id()), new TenantName(entity.name()), toStatus(entity.status()));
    }

    /**
     * Para la proyección de estado, que lee la columna sin reconstruir el agregado.
     */
    public static TenantStatus toStatus(String status) {
        return TenantStatus.valueOf(status);
    }
}
