package co.edu.uco.seguridad.shared.audit;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Puerto de la evidencia de auditoría de operaciones administrativas (HU-021). Vive en
 * {@code shared} — a diferencia de {@code AccessAuditRepository} (propio de {@code authorization}) —
 * porque lo consumen casos de uso de varios slices (`assignments`, `authorization`); el adaptador
 * concreto no vive aquí, solo el contrato.
 */
public interface AdministrationAuditRepository {

    Mono<Void> save(AdministrationEvent event);

    Flux<AdministrationEvent> findByCorrelationId(String correlationId);
}
