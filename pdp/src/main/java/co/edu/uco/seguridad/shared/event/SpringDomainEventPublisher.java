package co.edu.uco.seguridad.shared.event;

import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import org.springframework.context.ApplicationEventPublisher;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * Adaptador sobre el {@link ApplicationEventPublisher} de Spring: entrega síncrona, en el mismo
 * proceso, a cualquier {@code @EventListener} registrado — no el Event Publication Registry de
 * Spring Modulith, que no tiene backend para SurrealDB (ver ADR-017).
 */
public final class SpringDomainEventPublisher implements DomainEventPublisher {

    private final ApplicationEventPublisher events;

    public SpringDomainEventPublisher(ApplicationEventPublisher events) {
        this.events = Objects.requireNonNull(events, RequiredArgumentMessages.SPRING_EVENT_PUBLISHER);
    }

    @Override
    public Mono<Void> publish(DomainEvent event) {
        return Mono.fromRunnable(() -> events.publishEvent(event));
    }
}
