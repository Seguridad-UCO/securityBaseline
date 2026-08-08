package co.edu.uco.seguridad.pdp.aplicaciones.application.port.primary.dto.response;

import co.edu.uco.seguridad.pdp.commons.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.ApplicationName;
import co.edu.uco.seguridad.pdp.commons.TenantId;

import java.time.Instant;
import java.util.Objects;

/**
 * DTO de salida del puerto primario: proyección de una aplicación registrada.
 *
 * <p>Es la salida del módulo, no la entidad de dominio: un llamador externo recibe esta proyección
 * y nunca el agregado {@code Application} en sí.</p>
 */
public record RegisteredApplicationResponse(ApplicationId id, TenantId tenantId, ApplicationName name,
                                            Instant registeredAt) {

    public RegisteredApplicationResponse {
        Objects.requireNonNull(id, "se requiere id de aplicación");
        Objects.requireNonNull(tenantId, "se requiere id de inquilino");
        Objects.requireNonNull(name, "se requiere nombre de aplicación");
        Objects.requireNonNull(registeredAt, "se requiere instante de registro");
    }
}
