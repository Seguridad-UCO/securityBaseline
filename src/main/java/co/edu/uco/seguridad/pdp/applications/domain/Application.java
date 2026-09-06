package co.edu.uco.seguridad.pdp.applications.domain;

import co.edu.uco.seguridad.pdp.applications.domain.model.ApplicationBaseUrl;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationName;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;

import java.time.Instant;
import java.util.Objects;

/**
 * Entidad de aplicación: Java puro, inmutable, sin setters ni anotaciones de framework.
 *
 * <p>{@code baseUrl} es el origen de la aplicación externa que este catálogo protege; los recursos
 * registrados bajo esta aplicación ({@link co.edu.uco.seguridad.pdp.resources.domain.ProtectedResource})
 * son rutas relativas a esa base — el endpoint completo es {@code baseUrl + path}.</p>
 */
public record Application(ApplicationId id, TenantId tenantId, ApplicationName name, String description,
        ApplicationBaseUrl baseUrl, Instant registeredAt) {

    private static final int MAX_DESCRIPTION_LENGTH = 500;

    public Application {
        Objects.requireNonNull(id, RequiredArgumentMessages.APPLICATION_ID);
        Objects.requireNonNull(tenantId, RequiredArgumentMessages.TENANT_ID);
        Objects.requireNonNull(name, RequiredArgumentMessages.APPLICATION_NAME);
        Objects.requireNonNull(baseUrl, RequiredArgumentMessages.APPLICATION_BASE_URL);
        Objects.requireNonNull(registeredAt, RequiredArgumentMessages.REGISTERED_AT);
        description = description == null ? "" : description.trim();
        if (description.length() > MAX_DESCRIPTION_LENGTH) {
            description = description.substring(0, MAX_DESCRIPTION_LENGTH);
        }
    }

    public static Application register(ApplicationId id, TenantId tenantId, ApplicationName name,
            String description, ApplicationBaseUrl baseUrl, Instant registeredAt) {
        return new Application(id, tenantId, name, description, baseUrl, registeredAt);
    }
}
