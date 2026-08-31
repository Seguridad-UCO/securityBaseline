package co.edu.uco.seguridad.pdp.resources.application.primaryport.response;

import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.ResourceId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.resources.domain.model.HttpVerb;
import co.edu.uco.seguridad.pdp.resources.domain.model.ResourcePath;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;

import java.time.Instant;
import java.util.Objects;

/** DTO de salida del puerto primario: proyección de un endpoint protegido registrado. */
public record RegisteredProtectedResourceResponse(ResourceId id, ApplicationId applicationId, TenantId tenantId,
        ResourcePath path, HttpVerb method, Instant registeredAt) {

    public RegisteredProtectedResourceResponse {
        Objects.requireNonNull(id, RequiredArgumentMessages.RESOURCE_ID);
        Objects.requireNonNull(applicationId, RequiredArgumentMessages.APPLICATION_ID);
        Objects.requireNonNull(tenantId, RequiredArgumentMessages.TENANT_ID);
        Objects.requireNonNull(path, RequiredArgumentMessages.RESOURCE_PATH);
        Objects.requireNonNull(method, RequiredArgumentMessages.HTTP_METHOD);
        Objects.requireNonNull(registeredAt, RequiredArgumentMessages.REGISTERED_AT);
    }
}
