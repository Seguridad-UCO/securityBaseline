package co.edu.uco.seguridad.pdp.recursos.application.port.primary.dto.request;

import co.edu.uco.seguridad.crosscutting.messages.RequiredArgumentMessages;
import co.edu.uco.seguridad.pdp.commons.ApplicationName;
import co.edu.uco.seguridad.pdp.commons.TenantId;
import co.edu.uco.seguridad.pdp.recursos.domain.ActionCode;
import co.edu.uco.seguridad.pdp.recursos.domain.ResourceCode;

import java.util.Objects;

/**
 * DTO de entrada del puerto primario: registrar una aplicación protegida junto con su primer recurso.
 *
 * <p>Ningún {@code String} sobrevive hasta aquí: quien construye el DTO ya ha convertido la entrada bruta
 * en objetos de valor, por lo que el caso de uso no puede recibir algo malformado.</p>
 */
public record RegisterProtectedApplicationRequest(TenantId tenantId,
                                                  ApplicationName applicationName,
                                                  ResourceCode resourceCode,
                                                  ActionCode action) {

    public RegisterProtectedApplicationRequest {
        Objects.requireNonNull(tenantId, RequiredArgumentMessages.TENANT_ID);
        Objects.requireNonNull(applicationName, RequiredArgumentMessages.APPLICATION_NAME);
        Objects.requireNonNull(resourceCode, RequiredArgumentMessages.RESOURCE_CODE);
        Objects.requireNonNull(action, RequiredArgumentMessages.ACTION_CODE);
    }
}
