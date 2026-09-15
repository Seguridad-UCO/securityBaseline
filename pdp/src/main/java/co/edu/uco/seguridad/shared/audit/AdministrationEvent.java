package co.edu.uco.seguridad.shared.audit;

import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.shared.event.DomainEvent;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** La evidencia correlacionada de una operación administrativa (HU-021). */
public record AdministrationEvent(UUID eventId, String correlationId, TenantId tenantId, ApplicationId applicationId,
        String subject, AdministrationOperation operation, AdministrationOutcome outcome, Instant occurredOn)
        implements DomainEvent {

    public AdministrationEvent {
        Objects.requireNonNull(eventId, RequiredArgumentMessages.EVENT_ID);
        Objects.requireNonNull(correlationId, RequiredArgumentMessages.CORRELATION_ID);
        Objects.requireNonNull(tenantId, RequiredArgumentMessages.TENANT_ID);
        Objects.requireNonNull(applicationId, RequiredArgumentMessages.APPLICATION_ID);
        Objects.requireNonNull(subject, RequiredArgumentMessages.SUBJECT);
        Objects.requireNonNull(operation, RequiredArgumentMessages.ADMINISTRATION_OPERATION);
        Objects.requireNonNull(outcome, RequiredArgumentMessages.ADMINISTRATION_OUTCOME);
        Objects.requireNonNull(occurredOn, RequiredArgumentMessages.EVENT_OCCURRED_ON);
    }
}
