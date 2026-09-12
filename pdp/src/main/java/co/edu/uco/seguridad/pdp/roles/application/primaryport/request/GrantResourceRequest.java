package co.edu.uco.seguridad.pdp.roles.application.primaryport.request;

import co.edu.uco.seguridad.pdp.commons.model.ResourceId;
import co.edu.uco.seguridad.pdp.commons.model.RoleId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;

import java.util.Objects;

/** Entrada tipada de GrantResourceToRoleUseCase. El inquilino es el del principal: acota qué roles existen para él. */
public record GrantResourceRequest(TenantId tenantId, RoleId roleId, ResourceId resourceId) {

    public GrantResourceRequest {
        Objects.requireNonNull(tenantId, RequiredArgumentMessages.TENANT_ID);
        Objects.requireNonNull(roleId, RequiredArgumentMessages.ROLE_ID);
        Objects.requireNonNull(resourceId, RequiredArgumentMessages.RESOURCE_ID);
    }
}
