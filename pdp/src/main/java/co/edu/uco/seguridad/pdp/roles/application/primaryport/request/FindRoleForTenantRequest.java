package co.edu.uco.seguridad.pdp.roles.application.primaryport.request;

import co.edu.uco.seguridad.pdp.commons.model.RoleId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;

import java.util.Objects;

public record FindRoleForTenantRequest(RoleId roleId, TenantId tenantId) {
    public FindRoleForTenantRequest {
        Objects.requireNonNull(roleId);
        Objects.requireNonNull(tenantId);
    }
}
