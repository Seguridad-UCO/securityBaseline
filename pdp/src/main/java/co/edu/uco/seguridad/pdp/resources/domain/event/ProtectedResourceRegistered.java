package co.edu.uco.seguridad.pdp.resources.domain.event;

import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.ResourceId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.resources.domain.model.HttpVerb;
import co.edu.uco.seguridad.pdp.resources.domain.ProtectedResource;
import co.edu.uco.seguridad.pdp.resources.domain.model.ResourcePath;
import co.edu.uco.seguridad.shared.event.DomainEvent;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;

import java.time.Instant;
import java.util.Objects;

/**
 * Se registró un recurso protegido. Sustituye la llamada directa a {@code AuditPort} desde el caso de
 * uso: quien necesite reaccionar a este hecho (auditoría hoy, a futuro un PEP/PDP real) escucha este
 * evento en vez de que el caso de uso conozca a cada interesado (ver ADR-0002).
 */
public record ProtectedResourceRegistered(ResourceId resourceId, ApplicationId applicationId, TenantId tenantId,
        ResourcePath path, HttpVerb method, Instant occurredOn) implements DomainEvent {

    public ProtectedResourceRegistered {
        Objects.requireNonNull(resourceId, RequiredArgumentMessages.RESOURCE_ID);
        Objects.requireNonNull(applicationId, RequiredArgumentMessages.APPLICATION_ID);
        Objects.requireNonNull(tenantId, RequiredArgumentMessages.TENANT_ID);
        Objects.requireNonNull(path, RequiredArgumentMessages.RESOURCE_PATH);
        Objects.requireNonNull(method, RequiredArgumentMessages.HTTP_METHOD);
        Objects.requireNonNull(occurredOn, RequiredArgumentMessages.EVENT_OCCURRED_ON);
    }

    public static ProtectedResourceRegistered of(ProtectedResource resource) {
        return new ProtectedResourceRegistered(resource.id(), resource.applicationId(), resource.tenantId(),
                resource.path(), resource.method(), resource.registeredAt());
    }
}
