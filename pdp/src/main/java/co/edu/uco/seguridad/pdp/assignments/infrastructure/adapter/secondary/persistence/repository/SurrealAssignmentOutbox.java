package co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.secondary.persistence.repository;

import co.edu.uco.seguridad.pdp.assignments.application.event.AssignmentChangedEventV1;
import co.edu.uco.seguridad.pdp.assignments.application.secondaryport.event.AssignmentOutbox;
import co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.secondary.persistence.schema.AssignmentOutboxSchema;
import co.edu.uco.seguridad.shared.persistence.surrealdb.SurrealDbClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/** Adaptador Surreal de la outbox. El JSON se escribe una vez y nunca se recompone al reintentar. */
public final class SurrealAssignmentOutbox implements AssignmentOutbox {
    private final SurrealDbClient client;
    private final ObjectMapper mapper;
    public SurrealAssignmentOutbox(SurrealDbClient client, ObjectMapper mapper) {
        this.client = Objects.requireNonNull(client); this.mapper = Objects.requireNonNull(mapper);
    }
    @Override public Mono<Void> enqueue(AssignmentChangedEventV1 event) {
        return client.execute("CREATE type::record('%s', $id) SET payload = $payload, attempts = 0, nextAttemptOn = <datetime>$now, publishedOn = NONE, technicalError = NONE;"
                        .formatted(AssignmentOutboxSchema.TABLE),
                Map.of("id", event.eventId(), "payload", mapper.writeValueAsString(event), "now", event.occurredOn().toString())).then();
    }
    @Override public Flux<PendingAssignmentEvent> pending(Instant now) {
        return client.execute("SELECT * FROM %s WHERE publishedOn = NONE AND nextAttemptOn <= <datetime>$now ORDER BY nextAttemptOn LIMIT 100;"
                        .formatted(AssignmentOutboxSchema.TABLE), Map.of("now", now.toString()))
                .flatMapMany(rows -> Flux.fromIterable(rows.getFirst().valueStream().toList()))
                .map(this::toPending);
    }
    @Override public Mono<Void> published(String eventId, Instant publishedOn) {
        return client.execute("UPDATE type::record('%s', $id) SET publishedOn = <datetime>$publishedOn, technicalError = NONE;"
                .formatted(AssignmentOutboxSchema.TABLE), Map.of("id", eventId, "publishedOn", publishedOn.toString())).then();
    }
    @Override public Mono<Void> retry(String eventId, int attempts, Instant nextAttemptOn, String error) {
        return client.execute("UPDATE type::record('%s', $id) SET attempts = $attempts, nextAttemptOn = <datetime>$nextAttemptOn, technicalError = $error;"
                .formatted(AssignmentOutboxSchema.TABLE), Map.of("id", eventId, "attempts", Integer.toString(attempts),
                        "nextAttemptOn", nextAttemptOn.toString(), "error", error == null ? "unknown" : error)).then();
    }
    private PendingAssignmentEvent toPending(JsonNode row) {
        return new PendingAssignmentEvent(mapper.readValue(row.path("payload").asString(), AssignmentChangedEventV1.class), row.path("attempts").asInt());
    }
}
