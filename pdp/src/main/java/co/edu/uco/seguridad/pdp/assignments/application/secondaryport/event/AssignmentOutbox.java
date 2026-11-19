package co.edu.uco.seguridad.pdp.assignments.application.secondaryport.event;

import co.edu.uco.seguridad.pdp.assignments.application.event.AssignmentChangedEventV1;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

/** Puerto de outbox. Su persistencia forma parte de la misma unidad de escritura que el cambio. */
public interface AssignmentOutbox {
    Mono<Void> enqueue(AssignmentChangedEventV1 event);
    Flux<PendingAssignmentEvent> pending(Instant now);
    Mono<Void> published(String eventId, Instant publishedOn);
    Mono<Void> retry(String eventId, int attempts, Instant nextAttemptOn, String technicalError);

    record PendingAssignmentEvent(AssignmentChangedEventV1 event, int attempts) { }
}
