package co.edu.uco.seguridad.pdp.authorization.domain.serviceidentity;

import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.ResourceId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Autorización de una identidad técnica. Puede otorgar un recurso concreto o un rol técnico
 * de la aplicación destino. El rol nunca se asigna globalmente a una identidad: cada grant
 * conserva la identidad que lo recibió.
 */
public record ServiceAccessGrant(UUID id, TenantId tenantId, ServiceIdentityId serviceIdentityId,
                                 ApplicationId targetApplicationId,
                                 ResourceId resourceId, UUID technicalRoleId, String technicalRole,
                                 ServiceAccessStatus status,
                                 Instant requestedAt, Instant decidedAt) {
    public ServiceAccessGrant {
        Objects.requireNonNull(id);
        Objects.requireNonNull(tenantId);
        Objects.requireNonNull(serviceIdentityId);
        Objects.requireNonNull(targetApplicationId);
        Objects.requireNonNull(technicalRole);
        if (resourceId == null && technicalRoleId == null)
            throw new IllegalArgumentException("el acceso debe indicar recurso o rol técnico");
        Objects.requireNonNull(status);
        Objects.requireNonNull(requestedAt);
    }

    public ServiceAccessGrant approve(Instant at) {
        return new ServiceAccessGrant(id, tenantId, serviceIdentityId, targetApplicationId, resourceId, technicalRoleId, technicalRole,
                ServiceAccessStatus.APPROVED, requestedAt, Objects.requireNonNull(at));
    }

    public ServiceAccessGrant reject(Instant at) {
        return new ServiceAccessGrant(id, tenantId, serviceIdentityId, targetApplicationId, resourceId, technicalRoleId, technicalRole,
                ServiceAccessStatus.REJECTED, requestedAt, Objects.requireNonNull(at));
    }

    public ServiceAccessGrant revoke(Instant at) {
        return new ServiceAccessGrant(id, tenantId, serviceIdentityId, targetApplicationId, resourceId, technicalRoleId, technicalRole,
                ServiceAccessStatus.REVOKED, requestedAt, Objects.requireNonNull(at));
    }
}
