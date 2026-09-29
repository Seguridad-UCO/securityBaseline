package co.edu.uco.seguridad.pdp.roles.domain.rule.model;

import co.edu.uco.seguridad.pdp.commons.model.RoleId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;

import java.util.Objects;

/**
 * Hecho ya resuelto para RoleMustExistForTenantRule: si el rol existe para ese inquilino.
 */
public record RoleExistence(RoleId roleId, TenantId tenantId, boolean registered) {

    public RoleExistence {
        Objects.requireNonNull(roleId, RequiredArgumentMessages.ROLE_ID);
        Objects.requireNonNull(tenantId, RequiredArgumentMessages.TENANT_ID);
    }
}
