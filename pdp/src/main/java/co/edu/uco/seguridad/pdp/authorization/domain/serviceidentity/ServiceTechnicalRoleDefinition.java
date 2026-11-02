package co.edu.uco.seguridad.pdp.authorization.domain.serviceidentity;

import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Rol técnico definido por la aplicación propietaria de los recursos, exclusivo para M2M.
 */
public record ServiceTechnicalRoleDefinition(UUID id, TenantId tenantId, ApplicationId applicationId,
                                             String code, String name, Instant createdAt) {
    public ServiceTechnicalRoleDefinition {
        Objects.requireNonNull(id);
        Objects.requireNonNull(tenantId);
        Objects.requireNonNull(applicationId);
        Objects.requireNonNull(code);
        Objects.requireNonNull(name);
        Objects.requireNonNull(createdAt);
        if (code.isBlank() || name.isBlank())
            throw new IllegalArgumentException("código y nombre del rol técnico son requeridos");
        if (ServiceTechnicalRole.SERVICE_CONSUMER.name().equalsIgnoreCase(code)) {
            throw new IllegalArgumentException("SERVICE_CONSUMER es el rol técnico reservado para accesos por recurso");
        }
    }
}
