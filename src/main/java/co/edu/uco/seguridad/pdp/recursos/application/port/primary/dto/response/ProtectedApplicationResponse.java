package co.edu.uco.seguridad.pdp.recursos.application.port.primary.dto.response;

import co.edu.uco.seguridad.crosscutting.messages.RequiredArgumentMessages;
import co.edu.uco.seguridad.pdp.commons.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.ApplicationName;
import co.edu.uco.seguridad.pdp.commons.ResourceId;
import co.edu.uco.seguridad.pdp.commons.TenantId;
import co.edu.uco.seguridad.pdp.recursos.domain.ActionCode;
import co.edu.uco.seguridad.pdp.recursos.domain.ResourceCode;

import java.time.Instant;
import java.util.Objects;

/**
 * DTO de salida del puerto primario: fila del catálogo de aplicaciones protegidas.
 *
 * <p>Es la salida del caso de uso, no la carga HTTP: el adaptador primario la proyecta a su propio
 * DTO de respuesta para que el contrato de API pueda cambiar sin tocar el núcleo.</p>
 */
public record ProtectedApplicationResponse(ApplicationId applicationId,
                                           ResourceId resourceId,
                                           TenantId tenantId,
                                           ApplicationName applicationName,
                                           ResourceCode resourceCode,
                                           ActionCode action,
                                           Instant registeredAt) {

    public ProtectedApplicationResponse {
        Objects.requireNonNull(applicationId, RequiredArgumentMessages.APPLICATION_ID);
        Objects.requireNonNull(resourceId, RequiredArgumentMessages.RESOURCE_ID);
        Objects.requireNonNull(tenantId, RequiredArgumentMessages.TENANT_ID);
        Objects.requireNonNull(applicationName, RequiredArgumentMessages.APPLICATION_NAME);
        Objects.requireNonNull(resourceCode, RequiredArgumentMessages.RESOURCE_CODE);
        Objects.requireNonNull(action, RequiredArgumentMessages.ACTION_CODE);
        Objects.requireNonNull(registeredAt, RequiredArgumentMessages.REGISTERED_AT);
    }
}
