package co.edu.uco.seguridad.shared.event;

import reactor.core.publisher.Mono;

/**
 * Puerto secundario para publicar un evento de dominio. El caso de uso pide la publicación sin saber
 * si el mecanismo detrás es el Event Publication Registry de Spring Modulith, un bus externo, o un
 * dummy de prueba — igual que {@code AuditPort} antes de esta decisión (ver ADR-0002).
 */
@FunctionalInterface
public interface DomainEventPublisher {

    Mono<Void> publish(DomainEvent event);
}
