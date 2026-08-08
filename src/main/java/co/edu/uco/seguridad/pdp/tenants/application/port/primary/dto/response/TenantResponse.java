package co.edu.uco.seguridad.pdp.tenants.application.port.primary.dto.response;

import co.edu.uco.seguridad.pdp.commons.TenantId;
import co.edu.uco.seguridad.pdp.tenants.TenantStatus;

import java.util.Objects;

/**
 * DTO de salida del puerto primario: proyección inmutable que otros módulos reciben.
 *
 * <p>La entidad inquilino nunca sale del módulo. {@link TenantStatus} es un tipo publicado del módulo.</p>
 */
public record TenantResponse(TenantId id, TenantStatus status) {

    public TenantResponse {
        Objects.requireNonNull(id, "se requiere id de inquilino");
        Objects.requireNonNull(status, "se requiere estado del inquilino");
    }

    public boolean active() {
        return status.allowsRegistration();
    }
}
