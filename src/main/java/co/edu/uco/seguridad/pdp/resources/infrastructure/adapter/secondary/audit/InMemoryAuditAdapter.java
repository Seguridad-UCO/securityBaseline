package co.edu.uco.seguridad.pdp.resources.infrastructure.adapter.secondary.audit;

import co.edu.uco.seguridad.pdp.resources.domain.event.ProtectedResourceRegistered;
import org.springframework.context.event.EventListener;

import java.time.Instant;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Adaptador de auditoría ficticio sustituible (criterio 07): registra solo identificadores, nunca
 * la carga completa. Escucha {@link ProtectedResourceRegistered} con {@code @EventListener} síncrono,
 * no {@code @ApplicationModuleListener}, porque Spring Modulith no trae un backend de Event
 * Publication Registry para SurrealDB — ver ADR-017.
 */
public final class InMemoryAuditAdapter {

    /** Un hecho de auditoría. Solo identificadores: suficiente para reconstruir la acción, nada más. */
    public record AuditEvent(String event, String tenantId, String applicationId, String resourceId, Instant at) {
    }

    private final Queue<AuditEvent> events = new ConcurrentLinkedQueue<>();

    @EventListener
    void on(ProtectedResourceRegistered event) {
        events.add(new AuditEvent(
                "protected_application.registered",
                event.tenantId().value(),
                event.applicationId().value().toString(),
                event.resourceId().value().toString(),
                event.occurredOn()));
    }

    public List<AuditEvent> recorded() {
        return List.copyOf(events);
    }
}
