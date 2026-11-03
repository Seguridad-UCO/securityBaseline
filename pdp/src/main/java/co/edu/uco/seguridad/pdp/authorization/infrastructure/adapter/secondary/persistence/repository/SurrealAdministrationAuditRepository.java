package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.secondary.persistence.repository;

import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.secondary.persistence.entity.AdministrationEventEntity;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.secondary.persistence.mapper.AdministrationEventPersistenceMapper;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.secondary.persistence.schema.AdministrationEventSchema;
import co.edu.uco.seguridad.shared.audit.AdministrationAuditRepository;
import co.edu.uco.seguridad.shared.audit.AdministrationEvent;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import co.edu.uco.seguridad.shared.persistence.surrealdb.SurrealDbClient;
import co.edu.uco.seguridad.shared.persistence.surrealdb.SurrealRecordId;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tools.jackson.databind.JsonNode;

import java.util.Map;
import java.util.Objects;

import static java.util.Map.entry;

/**
 * Adaptador real sobre SurrealDB (HU-021), misma forma que {@code SurrealAccessAuditRepository}: lee
 * la fila en {@link AdministrationEventEntity} y delega en {@link AdministrationEventPersistenceMapper}.
 */
public final class SurrealAdministrationAuditRepository implements AdministrationAuditRepository {

    private final SurrealDbClient client;

    public SurrealAdministrationAuditRepository(SurrealDbClient client) {
        this.client = Objects.requireNonNull(client, RequiredArgumentMessages.SURREALDB_CLIENT);
    }

    @Override
    public Mono<Void> save(AdministrationEvent event) {
        AdministrationEventEntity entity = AdministrationEventPersistenceMapper.toEntity(event);
        return client.execute(
                        """
                                CREATE type::record('%s', $id) SET \
                                correlationId = $correlationId, tenantId = $tenantId, applicationId = $applicationId, \
                                subject = $subject, operation = $operation, outcome = $outcome, \
                                occurredOn = <datetime>$occurredOn;\
                                """.formatted(AdministrationEventSchema.TABLE),
                        Map.ofEntries(
                                entry("id", entity.id()),
                                entry("correlationId", entity.correlationId()),
                                entry("tenantId", entity.tenantId()),
                                entry("applicationId", entity.applicationId()),
                                entry("subject", entity.subject()),
                                entry("operation", entity.operation()),
                                entry("outcome", entity.outcome()),
                                entry("occurredOn", entity.occurredOn())))
                .then();
    }

    @Override
    public Flux<AdministrationEvent> findByCorrelationId(String correlationId) {
        return client.execute(
                        "SELECT * FROM %s WHERE correlationId = $correlationId;"
                                .formatted(AdministrationEventSchema.TABLE),
                        Map.of("correlationId", correlationId))
                .flatMapMany(results -> Flux.fromIterable(results.get(0).valueStream().toList()))
                .map(SurrealAdministrationAuditRepository::toDomain);
    }

    private static AdministrationEvent toDomain(JsonNode row) {
        AdministrationEventEntity entity = new AdministrationEventEntity(
                SurrealRecordId.idPart(row.path("id").asString()),
                row.path("correlationId").asString(),
                row.path("tenantId").asString(),
                row.path("applicationId").asString(),
                row.path("subject").asString(),
                row.path("operation").asString(),
                row.path("outcome").asString(),
                row.path("occurredOn").asString());
        return AdministrationEventPersistenceMapper.toDomain(entity);
    }
}
