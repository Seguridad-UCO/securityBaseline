package co.edu.uco.seguridad.pdp.applications.infrastructure.adapter.secondary.persistence.mapper;

import co.edu.uco.seguridad.pdp.applications.domain.Application;
import co.edu.uco.seguridad.pdp.applications.domain.model.ApplicationBaseUrl;
import co.edu.uco.seguridad.pdp.applications.domain.model.ApplicationCredentialHash;
import co.edu.uco.seguridad.pdp.applications.infrastructure.adapter.secondary.persistence.entity.ApplicationEntity;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationName;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;

import java.time.Instant;
import java.util.UUID;

/**
 * Traduce la fila de persistencia al agregado. Los value objects validan aquí: una fila corrupta se
 * rechaza al reconstruirla, no más adelante.
 */
public final class ApplicationPersistenceMapper {

    private ApplicationPersistenceMapper() {
    }

    public static Application toDomain(ApplicationEntity entity) {
        return new Application(
                new ApplicationId(UUID.fromString(entity.id())),
                new TenantId(entity.tenantId()),
                new ApplicationName(entity.name()),
                entity.description(),
                new ApplicationBaseUrl(entity.baseUrl()),
                new ApplicationCredentialHash(entity.credentialHash()),
                Instant.parse(entity.registeredAt()));
    }
}
