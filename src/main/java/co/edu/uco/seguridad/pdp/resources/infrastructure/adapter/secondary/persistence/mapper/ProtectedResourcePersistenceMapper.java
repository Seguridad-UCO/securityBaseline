package co.edu.uco.seguridad.pdp.resources.infrastructure.adapter.secondary.persistence.mapper;

import co.edu.uco.seguridad.pdp.commons.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.ApplicationName;
import co.edu.uco.seguridad.pdp.commons.ResourceId;
import co.edu.uco.seguridad.pdp.commons.TenantId;
import co.edu.uco.seguridad.pdp.resources.domain.ActionCode;
import co.edu.uco.seguridad.pdp.resources.domain.ProtectedResource;
import co.edu.uco.seguridad.pdp.resources.domain.ResourceCode;
import co.edu.uco.seguridad.pdp.resources.infrastructure.adapter.secondary.persistence.entity.ProtectedResourceEntity;

import java.util.UUID;

/** Traducción de formato entre fila de almacenamiento y entidad de dominio. */
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
}
