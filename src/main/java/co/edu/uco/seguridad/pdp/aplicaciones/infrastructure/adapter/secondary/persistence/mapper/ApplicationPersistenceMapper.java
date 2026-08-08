package co.edu.uco.seguridad.pdp.aplicaciones.infrastructure.adapter.secondary.persistence.mapper;

import co.edu.uco.seguridad.pdp.aplicaciones.domain.Application;
import co.edu.uco.seguridad.pdp.aplicaciones.infrastructure.adapter.secondary.persistence.entity.ApplicationEntity;
import co.edu.uco.seguridad.pdp.commons.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.ApplicationName;
import co.edu.uco.seguridad.pdp.commons.TenantId;

import java.util.UUID;

/**
 * Traduce entre la fila de almacenamiento y la entidad de dominio. Solo formato: no toma decisiones que una
 * regla podría tomar.
 */
public final class ApplicationPersistenceMapper {

    private ApplicationPersistenceMapper() {
    }

    public static Application toDomain(ApplicationEntity entity) {
        return new Application(
                new ApplicationId(UUID.fromString(entity.getId())),
                new TenantId(entity.getTenantId()),
                new ApplicationName(entity.getName()),
                entity.getRegisteredAt());
    }

    public static ApplicationEntity toEntity(Application application) {
        return new ApplicationEntity(
                application.id().value().toString(),
                application.tenantId().value(),
                application.name().value(),
                application.registeredAt());
    }
}
