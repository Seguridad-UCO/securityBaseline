package co.edu.uco.seguridad.pdp.assignments.infrastructure.events;

import co.edu.uco.seguridad.pdp.assignments.application.secondaryport.event.AssignmentOutbox;
import co.edu.uco.seguridad.shared.port.TimeProvider;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import reactor.core.publisher.Mono;

/** Entrega al menos una vez: un fallo del broker deja intacto el payload y agenda un reintento. */
public final class AssignmentOutboxDispatcher {
    private final AssignmentOutbox outbox; private final RabbitTemplate rabbit; private final AssignmentEventsProperties properties; private final TimeProvider time;
    public AssignmentOutboxDispatcher(AssignmentOutbox outbox, RabbitTemplate rabbit, AssignmentEventsProperties properties, TimeProvider time) { this.outbox = outbox; this.rabbit = rabbit; this.properties = properties; this.time = time; }
    @Scheduled(fixedDelayString = "${pdp.assignment-events.dispatch-delay:PT5S}")
    public Mono<Void> dispatch() {
        return outbox.pending(time.now()).concatMap(this::publishOrRetry)
                .then();
    }

    private Mono<Void> publishOrRetry(AssignmentOutbox.PendingAssignmentEvent item) {
        return publish(item)
                .then(outbox.published(item.event().eventId(), time.now()))
                .onErrorResume(error -> outbox.retry(item.event().eventId(), item.attempts() + 1,
                        time.now().plusSeconds(Math.min(300, 1L << Math.min(8, item.attempts()))), technicalError(error)));
    }

    /** Espera el confirm del broker; un envío iniciado no equivale a un mensaje durablemente aceptado. */
    private Mono<Void> publish(AssignmentOutbox.PendingAssignmentEvent item) {
        return Mono.defer(() -> {
            CorrelationData correlation = new CorrelationData(item.event().eventId());
            rabbit.convertAndSend(properties.exchange(), "assignment.changed." + item.event().applicationId(), item.event(), correlation);
            return Mono.fromFuture(correlation.getFuture())
                    .timeout(properties.confirmTimeout())
                    .flatMap(confirm -> {
                        if (!confirm.isAck()) {
                            return Mono.error(new IllegalStateException("RabbitMQ rechazó el mensaje: " + confirm.getReason()));
                        }
                        if (correlation.getReturned() != null) {
                            return Mono.error(new IllegalStateException("RabbitMQ devolvió el mensaje sin ruta vinculada"));
                        }
                        return Mono.empty();
                    });
        });
    }

    private static String technicalError(Throwable error) {
        String message = error.getMessage();
        return error.getClass().getSimpleName() + (message == null ? "" : ": " + message);
    }
}
