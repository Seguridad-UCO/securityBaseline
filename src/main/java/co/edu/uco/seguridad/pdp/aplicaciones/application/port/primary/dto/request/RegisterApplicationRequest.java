package co.edu.uco.seguridad.pdp.aplicaciones.application.port.primary.dto.request;

import co.edu.uco.seguridad.pdp.commons.ApplicationName;
import co.edu.uco.seguridad.pdp.commons.TenantId;

import java.util.Objects;

/**
 * DTO de entrada del puerto primario: intención tipada de registrar una aplicación para un inquilino.
 *
 * <p>Ningún {@code String} sobrevive hasta aquí: quien construye el DTO ya ha convertido la entrada bruta
 * en objetos de valor, por lo que el módulo no puede recibir algo malformado.</p>
 */
public record RegisterApplicationRequest(TenantId tenantId, ApplicationName name) {

    public RegisterApplicationRequest {
        Objects.requireNonNull(tenantId, "se requiere id de inquilino");
        Objects.requireNonNull(name, "se requiere nombre de aplicación");
    }
}
