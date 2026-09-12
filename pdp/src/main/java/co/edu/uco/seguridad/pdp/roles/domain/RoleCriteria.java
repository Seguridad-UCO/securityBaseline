package co.edu.uco.seguridad.pdp.roles.domain;

import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;

import java.util.Objects;

/**
 * Specification de consulta del catálogo visible para un inquilino: sus roles más los globales.
 * El inquilino es obligatorio a propósito: el aislamiento es una precondición, no una opción.
 */
public record RoleCriteria(TenantId tenantId) {

    public RoleCriteria {
        Objects.requireNonNull(tenantId, RequiredArgumentMessages.TENANT_ID);
    }

    public static RoleCriteria ofTenant(TenantId tenantId) {
        return new RoleCriteria(tenantId);
    }

    public boolean matches(Role role) {
        Objects.requireNonNull(role, RequiredArgumentMessages.ROLE);
        if (role.scope().isGlobal()) {
            return true;
        }
        return role.scope().tenantId().map(tenantId::equals).orElse(false);
    }
}
