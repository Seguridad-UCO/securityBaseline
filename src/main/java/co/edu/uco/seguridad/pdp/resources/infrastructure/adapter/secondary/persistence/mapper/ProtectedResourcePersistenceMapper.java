package co.edu.uco.seguridad.pdp.resources.infrastructure.adapter.secondary.persistence.mapper;

import co.edu.uco.seguridad.pdp.commons.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.ResourceId;
import co.edu.uco.seguridad.pdp.commons.TenantId;
import co.edu.uco.seguridad.pdp.resources.domain.HttpVerb;
import co.edu.uco.seguridad.pdp.resources.domain.ProtectedResource;
import co.edu.uco.seguridad.pdp.resources.domain.ResourcePath;
import co.edu.uco.seguridad.pdp.resources.infrastructure.adapter.secondary.persistence.entity.ProtectedResourceEntity;

import java.util.UUID;

/** Traducción de formato entre fila de almacenamiento y entidad de dominio. */
public final class ProtectedResourcePersistenceMapper {

    private ProtectedResourcePersistenceMapper() {
    }

    public static ProtectedResource toDomain(ProtectedResourceEntity entity) {
        return new ProtectedResource(
                new ResourceId(UUID.fromString(entity.id())),
                new ApplicationId(UUID.fromString(entity.applicationId())),
                new TenantId(entity.tenantId()),
                new ResourcePath(entity.path()),
                HttpVerb.parse(entity.method()),
                entity.registeredAt());
    }
}
