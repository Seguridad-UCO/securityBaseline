package co.edu.uco.seguridad.pdp.tenants.domain;

import co.edu.uco.seguridad.pdp.commons.TenantId;
import co.edu.uco.seguridad.pdp.tenants.TenantStatus;

import java.util.Objects;

/**
 * Entidad inquilino: Java puro, inmutable, sin setters y sin anotaciones de framework.
 */
public record Tenant(TenantId id, TenantStatus status) {

    public Tenant {
        Objects.requireNonNull(id, "se requiere id de inquilino");
        Objects.requireNonNull(status, "se requiere estado del inquilino");
    }

    public boolean isActive() {
        return status.allowsRegistration();
    }
}
