package co.edu.uco.seguridad.pdp.tenants.domain;

import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.tenants.domain.model.TenantName;
import co.edu.uco.seguridad.pdp.tenants.domain.model.TenantStatus;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;

import java.util.Objects;

/**
 * Entidad inquilino: Java puro, inmutable, sin setters y sin anotaciones de framework.
 */
public record Tenant(TenantId id, TenantName name, TenantStatus status) {

    public Tenant {
        Objects.requireNonNull(id, RequiredArgumentMessages.TENANT_ID);
        Objects.requireNonNull(name, RequiredArgumentMessages.TENANT_NAME);
        Objects.requireNonNull(status, RequiredArgumentMessages.TENANT_STATUS);
    }

    public static Tenant register(TenantId id, TenantName name) {
        return new Tenant(id, name, TenantStatus.ACTIVE);
    }

    public boolean isActive() {
        return status.allowsRegistration();
    }
}
