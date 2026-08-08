package co.edu.uco.seguridad.shared.event;

import org.springframework.context.ApplicationEventPublisher;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * Adaptador secundario (driven) sobre el {@link ApplicationEventPublisher} de Spring: entrega
 * síncrona, en el mismo proceso, a cualquier {@code @EventListener} registrado.
 *
 * <p>No es el Event Publication Registry de Spring Modulith. Esa infraestructura (entrega asíncrona
 * tras el commit, con seguimiento de qué listener aún no procesó un evento y reintento tras un
 * reinicio) requiere un almacén persistente — JDBC, JPA, MongoDB o Neo4j — y ninguno encaja mientras
 * la persistencia siga siendo dummy (ver ADR-0002 y ADR-0004). Este adaptador es la implementación
 * honesta para esta etapa: cumple el puerto {@link DomainEventPublisher} y puede sustituirse por una
 * respaldada por Modulith el día que exista un almacén real que la sostenga, sin que ningún caso de
 * uso cambie.</p>
 */
public final class SpringDomainEventPublisher implements DomainEventPublisher {

    private final ApplicationEventPublisher events;

    public SpringDomainEventPublisher(ApplicationEventPublisher events) {
        this.events = Objects.requireNonNull(events, "se requiere el publicador de eventos de Spring");
    }

    @Override
    public Mono<Void> publish(DomainEvent event) {
        return Mono.fromRunnable(() -> events.publishEvent(event));
    }
}
