package co.edu.uco.seguridad.pdp.tenants.domain;

import co.edu.uco.seguridad.pdp.commons.TenantId;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;

import java.util.Objects;

/**
 * Entidad inquilino: Java puro, inmutable, sin setters y sin anotaciones de framework.
 */
public record Tenant(TenantId id, TenantStatus status) {

    public Tenant {
        Objects.requireNonNull(id, RequiredArgumentMessages.TENANT_ID);
        Objects.requireNonNull(status, RequiredArgumentMessages.TENANT_STATUS);
    }

    public boolean isActive() {
        return status.allowsRegistration();
    }
}
