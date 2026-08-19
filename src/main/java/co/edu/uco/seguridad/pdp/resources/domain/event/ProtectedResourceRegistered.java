package co.edu.uco.seguridad.pdp.resources.domain.event;

import co.edu.uco.seguridad.pdp.commons.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.ResourceId;
import co.edu.uco.seguridad.pdp.commons.TenantId;
import co.edu.uco.seguridad.pdp.resources.domain.ActionCode;
import co.edu.uco.seguridad.pdp.resources.domain.ProtectedResource;
import co.edu.uco.seguridad.pdp.resources.domain.ResourceCode;
import co.edu.uco.seguridad.shared.event.DomainEvent;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;

import java.time.Instant;
import java.util.Objects;

/**
 * Se registró un recurso protegido. Sustituye la llamada directa a {@code AuditPort} desde el caso de
 * uso: quien necesite reaccionar a este hecho (auditoría hoy, a futuro un PEP/PDP real) escucha este
 * evento en vez de que el caso de uso conozca a cada interesado (ver ADR-0002).
 */
public record ProtectedResourceRegistered(ResourceId resourceId,
                                          ApplicationId applicationId,
                                          TenantId tenantId,
                                          ResourceCode resourceCode,
                                          ActionCode action,
                                          Instant occurredOn) implements DomainEvent {

    public ProtectedResourceRegistered {
        Objects.requireNonNull(resourceId, RequiredArgumentMessages.RESOURCE_ID);
        Objects.requireNonNull(applicationId, RequiredArgumentMessages.APPLICATION_ID);
        Objects.requireNonNull(tenantId, RequiredArgumentMessages.TENANT_ID);
        Objects.requireNonNull(resourceCode, RequiredArgumentMessages.RESOURCE_CODE);
        Objects.requireNonNull(action, RequiredArgumentMessages.ACTION_CODE);
        Objects.requireNonNull(occurredOn, RequiredArgumentMessages.EVENT_OCCURRED_ON);
    }

    public static ProtectedResourceRegistered of(ProtectedResource resource) {
        return new ProtectedResourceRegistered(
                resource.id(), resource.applicationId(), resource.tenantId(),
                resource.code(), resource.action(), resource.registeredAt());
    }
}
