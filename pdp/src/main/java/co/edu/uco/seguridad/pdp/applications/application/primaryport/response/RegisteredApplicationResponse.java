package co.edu.uco.seguridad.pdp.applications.application.primaryport.response;

import co.edu.uco.seguridad.pdp.applications.domain.model.ApplicationBaseUrl;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationName;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;

import java.time.Instant;
import java.util.Objects;

/**
 * DTO de salida del puerto primario: proyección de una aplicación registrada.
 *
 * <p>Es la salida del módulo, no la entidad de dominio: un llamador externo recibe esta proyección
 * y nunca el agregado {@code Application} en sí.</p>
 */
public record RegisteredApplicationResponse(ApplicationId id, TenantId tenantId, ApplicationName name,
                                            String description, ApplicationBaseUrl baseUrl, Instant registeredAt) {

    public RegisteredApplicationResponse {
        Objects.requireNonNull(id, RequiredArgumentMessages.APPLICATION_ID);
        Objects.requireNonNull(tenantId, RequiredArgumentMessages.TENANT_ID);
        Objects.requireNonNull(name, RequiredArgumentMessages.APPLICATION_NAME);
        Objects.requireNonNull(baseUrl, RequiredArgumentMessages.APPLICATION_BASE_URL);
        Objects.requireNonNull(registeredAt, RequiredArgumentMessages.REGISTERED_AT);
        description = description == null ? "" : description;
    }
}
