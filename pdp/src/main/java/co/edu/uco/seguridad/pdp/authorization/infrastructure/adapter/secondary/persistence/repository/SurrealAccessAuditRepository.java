package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.secondary.persistence.repository;

import co.edu.uco.seguridad.pdp.authorization.application.secondaryport.AccessAuditRepository;
import co.edu.uco.seguridad.pdp.authorization.domain.event.AccessEvent;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.secondary.persistence.entity.AccessEventEntity;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.secondary.persistence.mapper.AccessEventPersistenceMapper;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.secondary.persistence.schema.AccessEventSchema;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import co.edu.uco.seguridad.shared.persistence.surrealdb.SurrealDbClient;
import co.edu.uco.seguridad.shared.persistence.surrealdb.SurrealRecordId;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tools.jackson.databind.JsonNode;

import java.util.Map;
import java.util.Objects;

import static java.util.Map.entry;

/** Adaptador real sobre SurrealDB. Lee la fila en AccessEventEntity y delega en AccessEventPersistenceMapper. */
public final class SurrealAccessAuditRepository implements AccessAuditRepository {

    private final SurrealDbClient client;

    public SurrealAccessAuditRepository(SurrealDbClient client) {
        this.client = Objects.requireNonNull(client, RequiredArgumentMessages.SURREALDB_CLIENT);
    }

    @Override
    public Mono<Void> save(AccessEvent event) {
        AccessEventEntity entity = AccessEventPersistenceMapper.toEntity(event);
        return client.execute(
                        """
                        CREATE type::record('%s', $id) SET \
                        decisionId = $decisionId, requestId = $requestId, correlationId = $correlationId, \
                        tenantId = $tenantId, applicationId = $applicationId, subject = $subject, \
                        resourcePath = $resourcePath, action = $action, state = $state, reasonCode = $reasonCode, \
                        occurredOn = <datetime>$occurredOn;\
                        """.formatted(AccessEventSchema.TABLE),
                        Map.ofEntries(
                                entry("id", entity.id()),
                                entry("decisionId", entity.decisionId()),
                                entry("requestId", entity.requestId()),
                                entry("correlationId", entity.correlationId()),
                                entry("tenantId", entity.tenantId()),
                                entry("applicationId", entity.applicationId()),
                                entry("subject", entity.subject()),
                                entry("resourcePath", entity.resourcePath()),
                                entry("action", entity.action()),
                                entry("state", entity.state()),
                                entry("reasonCode", entity.reasonCode()),
                                entry("occurredOn", entity.occurredOn())))
                .then();
    }

    @Override
    public Flux<AccessEvent> findByCorrelationId(String correlationId) {
        return client.execute(
                        "SELECT * FROM %s WHERE correlationId = $correlationId;".formatted(AccessEventSchema.TABLE),
                        Map.of("correlationId", correlationId))
                .flatMapMany(results -> Flux.fromIterable(results.get(0).valueStream().toList()))
                .map(SurrealAccessAuditRepository::toDomain);
    }

    private static AccessEvent toDomain(JsonNode row) {
        AccessEventEntity entity = new AccessEventEntity(
                SurrealRecordId.idPart(row.path("id").asString()),
                row.path("decisionId").asString(),
                row.path("requestId").asString(),
                row.path("correlationId").asString(),
                row.path("tenantId").asString(),
                row.path("applicationId").asString(),
                row.path("subject").asString(),
                row.path("resourcePath").asString(),
                row.path("action").asString(),
                row.path("state").asString(),
                row.path("reasonCode").asString(),
                row.path("occurredOn").asString());
        return AccessEventPersistenceMapper.toDomain(entity);
    }
}
