package co.edu.uco.seguridad.pdp.roles.domain;

import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;

import java.util.Objects;

/**
 * Specification de consulta del catálogo visible para un inquilino: sus roles más los globales.
 * El inquilino es obligatorio a propósito: el aislamiento es una precondición, no una opción.
 */
public record RoleCriteria(TenantId tenantId, java.util.Optional<ApplicationId> applicationId) {

    public RoleCriteria(TenantId tenantId) {
        this(tenantId, java.util.Optional.empty());
    }

    public RoleCriteria {
        Objects.requireNonNull(tenantId, RequiredArgumentMessages.TENANT_ID);
        Objects.requireNonNull(applicationId, RequiredArgumentMessages.APPLICATION_ID);
    }

    public static RoleCriteria ofTenant(TenantId tenantId) {
        return new RoleCriteria(tenantId, java.util.Optional.empty());
    }

    /**
     * Consulta exclusivamente los roles con alcance de la aplicación indicada.
     */
    public static RoleCriteria ofApplication(TenantId tenantId, ApplicationId applicationId) {
        return new RoleCriteria(tenantId, java.util.Optional.of(applicationId));
    }

    public boolean matches(Role role) {
        Objects.requireNonNull(role, RequiredArgumentMessages.ROLE);
        if (applicationId.isPresent()) {
            return role.scope().tenantId().filter(tenantId::equals).isPresent()
                    && role.scope().applicationId().filter(applicationId.get()::equals).isPresent();
        }
        return role.scope().isGlobal() || role.scope().tenantId().map(tenantId::equals).orElse(false);
    }
}
