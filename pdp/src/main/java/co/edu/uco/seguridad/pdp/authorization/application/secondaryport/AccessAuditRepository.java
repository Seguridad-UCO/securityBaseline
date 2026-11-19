package co.edu.uco.seguridad.pdp.authorization.application.secondaryport;

import co.edu.uco.seguridad.pdp.authorization.domain.event.AccessEvent;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Puerto secundario de la evidencia de auditoría (HU-007). No pasa por {@code DomainEventPublisher}
 * a propósito — ver PLAN-HU-007.md §0: un {@code @EventListener} no puede esperar de forma segura un
 * {@code Mono} reactivo sin suscribirse a mano, que este proyecto prohíbe.
 */
public interface AccessAuditRepository {

    Mono<Void> save(AccessEvent event);

    Flux<AccessEvent> findByCorrelationId(String correlationId);
}
