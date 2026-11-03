package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.secondary.persistence.mapper;

import co.edu.uco.seguridad.pdp.authorization.domain.event.AccessEvent;
import co.edu.uco.seguridad.pdp.authorization.domain.model.DecisionState;
import co.edu.uco.seguridad.pdp.authorization.domain.model.ReasonCode;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.secondary.persistence.entity.AccessEventEntity;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.resources.domain.model.HttpVerb;
import co.edu.uco.seguridad.pdp.resources.domain.model.ResourcePath;

import java.time.Instant;
import java.util.UUID;

/**
 * Fila a dominio y de vuelta. Los value objects validan aquí.
 */
public final class AccessEventPersistenceMapper {

    private AccessEventPersistenceMapper() {
    }

    public static AccessEvent toDomain(AccessEventEntity entity) {
        return new AccessEvent(UUID.fromString(entity.id()), UUID.fromString(entity.decisionId()),
                entity.requestId(), entity.correlationId(), new TenantId(entity.tenantId()),
                ApplicationId.of(entity.applicationId()), entity.subject(), new ResourcePath(entity.resourcePath()),
                HttpVerb.valueOf(entity.action()), DecisionState.valueOf(entity.state()),
                ReasonCode.valueOf(entity.reasonCode()), Instant.parse(entity.occurredOn()));
    }

    public static AccessEventEntity toEntity(AccessEvent event) {
        return new AccessEventEntity(event.eventId().toString(), event.decisionId().toString(), event.requestId(),
                event.correlationId(), event.tenantId().value(), event.applicationId().value().toString(),
                event.subject(), event.resourcePath().value(), event.action().name(), event.state().name(),
                event.reasonCode().name(), event.occurredOn().toString());
    }
}
