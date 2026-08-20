package co.edu.uco.seguridad.pdp.resources.domain;

import co.edu.uco.seguridad.pdp.commons.AggregateRoot;
import co.edu.uco.seguridad.pdp.commons.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.ResourceId;
import co.edu.uco.seguridad.pdp.commons.TenantId;
import co.edu.uco.seguridad.pdp.resources.domain.event.ProtectedResourceRegistered;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;

import java.time.Instant;
import java.util.Objects;

/**
 * Un endpoint protegido de una aplicación: un método HTTP sobre una ruta relativa a la
 * {@code baseUrl} de esa aplicación.
 */
public record ProtectedResource(ResourceId id, ApplicationId applicationId, TenantId tenantId, ResourcePath path,
        HttpVerb method, Instant registeredAt) {

    public ProtectedResource {
        Objects.requireNonNull(id, RequiredArgumentMessages.RESOURCE_ID);
        Objects.requireNonNull(applicationId, RequiredArgumentMessages.APPLICATION_ID);
        Objects.requireNonNull(tenantId, RequiredArgumentMessages.TENANT_ID);
        Objects.requireNonNull(path, RequiredArgumentMessages.RESOURCE_PATH);
        Objects.requireNonNull(method, RequiredArgumentMessages.HTTP_METHOD);
        Objects.requireNonNull(registeredAt, RequiredArgumentMessages.REGISTERED_AT);
    }

    public static ProtectedResource register(ResourceId id, ApplicationId applicationId, TenantId tenantId,
            ResourcePath path, HttpVerb method, Instant registeredAt) {
        return new ProtectedResource(id, applicationId, tenantId, path, method, registeredAt);
    }

    /** Igual que {@link #register}, pero empareja el recurso con su evento — usar al escribir, no al leer. */
    public static AggregateRoot<ProtectedResource, ProtectedResourceRegistered> registerWithEvent(ResourceId id,
            ApplicationId applicationId, TenantId tenantId, ResourcePath path, HttpVerb method,
            Instant registeredAt) {
        ProtectedResource resource = register(id, applicationId, tenantId, path, method, registeredAt);
        return AggregateRoot.of(resource, ProtectedResourceRegistered.of(resource));
    }

    /** Identidad funcional de un endpoint: el mismo método sobre la misma ruta no puede registrarse dos veces. */
    public boolean isSameEndpointAs(ResourcePath otherPath, HttpVerb otherMethod) {
        return path.equals(otherPath) && method == otherMethod;
    }
}
