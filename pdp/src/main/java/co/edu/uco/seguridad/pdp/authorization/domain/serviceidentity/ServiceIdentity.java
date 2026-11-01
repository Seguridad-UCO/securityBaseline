package co.edu.uco.seguridad.pdp.authorization.domain.serviceidentity;

import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import java.time.Instant;
import java.util.Objects;

/** Identidad técnica perteneciente a una aplicación consumidora, distinta de usuarios y perfiles. */
public record ServiceIdentity(ServiceIdentityId id, TenantId tenantId, ApplicationId consumerApplicationId,
                              String name, String clientId, ServiceIdentityStatus status, Instant registeredAt) {
    public ServiceIdentity {
        Objects.requireNonNull(id); Objects.requireNonNull(tenantId); Objects.requireNonNull(consumerApplicationId);
        Objects.requireNonNull(name); Objects.requireNonNull(clientId); Objects.requireNonNull(status); Objects.requireNonNull(registeredAt);
        name = name.trim(); clientId = clientId.trim();
        if (name.isEmpty() || clientId.isEmpty()) throw new IllegalArgumentException("nombre y clientId son requeridos");
    }
    public ServiceIdentity withStatus(ServiceIdentityStatus next) {
        return new ServiceIdentity(id, tenantId, consumerApplicationId, name, clientId, next, registeredAt);
    }
}
