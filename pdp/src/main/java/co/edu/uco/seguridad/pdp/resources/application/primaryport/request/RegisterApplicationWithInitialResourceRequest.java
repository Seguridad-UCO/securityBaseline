package co.edu.uco.seguridad.pdp.resources.application.primaryport.request;

import co.edu.uco.seguridad.pdp.applications.domain.model.ApplicationBaseUrl;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationName;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.resources.domain.model.HttpVerb;
import co.edu.uco.seguridad.pdp.resources.domain.model.ResourcePath;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;

import java.util.Objects;

/**
 * DTO de entrada del puerto primario (HU-010): intención tipada de registrar una aplicación junto
 * con su recurso inicial, en una sola operación con compensación explícita si el segundo paso
 * falla.
 */
public record RegisterApplicationWithInitialResourceRequest(TenantId tenantId, ApplicationName name,
        String description, ApplicationBaseUrl baseUrl, ResourcePath resourcePath, HttpVerb resourceMethod) {

    public RegisterApplicationWithInitialResourceRequest {
        Objects.requireNonNull(tenantId, RequiredArgumentMessages.TENANT_ID);
        Objects.requireNonNull(name, RequiredArgumentMessages.APPLICATION_NAME);
        Objects.requireNonNull(baseUrl, RequiredArgumentMessages.APPLICATION_BASE_URL);
        Objects.requireNonNull(resourcePath, RequiredArgumentMessages.RESOURCE_PATH);
        Objects.requireNonNull(resourceMethod, RequiredArgumentMessages.HTTP_METHOD);
        description = description == null ? "" : description;
    }
}
