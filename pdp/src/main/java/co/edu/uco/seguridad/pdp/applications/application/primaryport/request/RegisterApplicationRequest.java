package co.edu.uco.seguridad.pdp.applications.application.primaryport.request;

import co.edu.uco.seguridad.pdp.applications.domain.model.ApplicationBaseUrl;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationName;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;

import java.util.Objects;

/**
 * DTO de entrada del puerto primario: intención tipada de registrar una aplicación para un inquilino.
 *
 * <p>Ningún {@code String} sobrevive hasta aquí salvo {@code description}, que es texto libre sin
 * invariante propio más allá de un tope de longitud que la entidad ya aplica.</p>
 */
public record RegisterApplicationRequest(TenantId tenantId, ApplicationName name, String description,
                                         ApplicationBaseUrl baseUrl) {

    public RegisterApplicationRequest {
        Objects.requireNonNull(tenantId, RequiredArgumentMessages.TENANT_ID);
        Objects.requireNonNull(name, RequiredArgumentMessages.APPLICATION_NAME);
        Objects.requireNonNull(baseUrl, RequiredArgumentMessages.APPLICATION_BASE_URL);
    }
}
