package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.secondary.persistence.mapper;

import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.secondary.persistence.entity.AdministrationEventEntity;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.shared.audit.AdministrationEvent;
import co.edu.uco.seguridad.shared.audit.AdministrationOperation;
import co.edu.uco.seguridad.shared.audit.AdministrationOutcome;

import java.time.Instant;
import java.util.UUID;

/**
 * Fila a dominio y de vuelta (HU-021). Los value objects validan aquí.
 */
public final class AdministrationEventPersistenceMapper {

    private AdministrationEventPersistenceMapper() {
    }

    public static AdministrationEvent toDomain(AdministrationEventEntity entity) {
        return new AdministrationEvent(UUID.fromString(entity.id()), entity.correlationId(),
                new TenantId(entity.tenantId()), ApplicationId.of(entity.applicationId()), entity.subject(),
                AdministrationOperation.valueOf(entity.operation()), AdministrationOutcome.valueOf(entity.outcome()),
                Instant.parse(entity.occurredOn()));
    }

    public static AdministrationEventEntity toEntity(AdministrationEvent event) {
        return new AdministrationEventEntity(event.eventId().toString(), event.correlationId(),
                event.tenantId().value(), event.applicationId().value().toString(), event.subject(),
                event.operation().name(), event.outcome().name(), event.occurredOn().toString());
    }
}
