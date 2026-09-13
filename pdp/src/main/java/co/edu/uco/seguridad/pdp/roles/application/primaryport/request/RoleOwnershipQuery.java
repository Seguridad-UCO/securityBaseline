package co.edu.uco.seguridad.pdp.roles.application.primaryport.request;

import co.edu.uco.seguridad.pdp.commons.model.RoleId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;

import java.util.Objects;

/** Entrada de {@code RoleMustExistForTenantValidator} (HU-011): el rol y el inquilino que pregunta por él. */
public record RoleOwnershipQuery(RoleId roleId, TenantId tenantId) {

    public RoleOwnershipQuery {
        Objects.requireNonNull(roleId, RequiredArgumentMessages.ROLE_ID);
        Objects.requireNonNull(tenantId, RequiredArgumentMessages.TENANT_ID);
    }
}
