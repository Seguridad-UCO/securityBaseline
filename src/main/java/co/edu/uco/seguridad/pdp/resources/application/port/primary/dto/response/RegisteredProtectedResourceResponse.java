package co.edu.uco.seguridad.pdp.resources.application.port.primary.dto.response;

import co.edu.uco.seguridad.pdp.commons.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.ResourceId;
import co.edu.uco.seguridad.pdp.commons.TenantId;
import co.edu.uco.seguridad.pdp.resources.domain.HttpVerb;
import co.edu.uco.seguridad.pdp.resources.domain.ResourcePath;
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
