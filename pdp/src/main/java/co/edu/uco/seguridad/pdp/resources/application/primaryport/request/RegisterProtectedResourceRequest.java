package co.edu.uco.seguridad.pdp.resources.application.primaryport.request;

import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.resources.domain.model.HttpVerb;
import co.edu.uco.seguridad.pdp.resources.domain.model.ResourcePath;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;

import java.util.Objects;

/**
 * DTO de entrada del puerto primario: intención tipada de registrar un endpoint protegido.
 */
public record RegisterProtectedResourceRequest(TenantId tenantId, ApplicationId applicationId, ResourcePath path,
                                               HttpVerb method) {

    public RegisterProtectedResourceRequest {
        Objects.requireNonNull(tenantId, RequiredArgumentMessages.TENANT_ID);
        Objects.requireNonNull(applicationId, RequiredArgumentMessages.APPLICATION_ID);
        Objects.requireNonNull(path, RequiredArgumentMessages.RESOURCE_PATH);
        Objects.requireNonNull(method, RequiredArgumentMessages.HTTP_METHOD);
    }
}
