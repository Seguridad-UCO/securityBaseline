package co.edu.uco.seguridad.pdp.recursos.infrastructure.adapter.secondary.persistence.mapper;

import co.edu.uco.seguridad.pdp.commons.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.ApplicationName;
import co.edu.uco.seguridad.pdp.commons.ResourceId;
import co.edu.uco.seguridad.pdp.commons.TenantId;
import co.edu.uco.seguridad.pdp.recursos.domain.ActionCode;
import co.edu.uco.seguridad.pdp.recursos.domain.ProtectedResource;
import co.edu.uco.seguridad.pdp.recursos.domain.ResourceCode;
import co.edu.uco.seguridad.pdp.recursos.infrastructure.adapter.secondary.persistence.entity.ProtectedResourceEntity;

import java.util.UUID;

/**
 * Traducción de formato entre fila de almacenamiento y entidad de dominio, en ambas direcciones y nada más.
 */
public final class ProtectedResourcePersistenceMapper {

    private ProtectedResourcePersistenceMapper() {
    }

    public static ProtectedResource toDomain(ProtectedResourceEntity entity) {
        return new ProtectedResource(
                new ResourceId(UUID.fromString(entity.getId())),
                new ApplicationId(UUID.fromString(entity.getApplicationId())),
                new TenantId(entity.getTenantId()),
                new ApplicationName(entity.getApplicationName()),
                new ResourceCode(entity.getResourceCode()),
                new ActionCode(entity.getAction()),
                entity.getRegisteredAt());
    }

    public static ProtectedResourceEntity toEntity(ProtectedResource resource) {
        return new ProtectedResourceEntity(
                resource.id().value().toString(),
                resource.applicationId().value().toString(),
                resource.tenantId().value(),
                resource.applicationName().value(),
                resource.code().value(),
                resource.action().value(),
                resource.registeredAt());
    }
}
