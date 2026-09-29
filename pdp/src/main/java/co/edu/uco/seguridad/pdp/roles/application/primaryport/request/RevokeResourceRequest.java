package co.edu.uco.seguridad.pdp.roles.application.primaryport.request;

import co.edu.uco.seguridad.pdp.commons.model.ResourceId;
import co.edu.uco.seguridad.pdp.commons.model.RoleId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;

import java.util.Objects;

/**
 * Solicitud para retirar un recurso de un rol existente.
 */
public record RevokeResourceRequest(TenantId tenantId, RoleId roleId, ResourceId resourceId) {
    public RevokeResourceRequest {
        Objects.requireNonNull(tenantId, RequiredArgumentMessages.TENANT_ID);
        Objects.requireNonNull(roleId, RequiredArgumentMessages.ROLE_ID);
        Objects.requireNonNull(resourceId, RequiredArgumentMessages.RESOURCE_ID);
    }
}
