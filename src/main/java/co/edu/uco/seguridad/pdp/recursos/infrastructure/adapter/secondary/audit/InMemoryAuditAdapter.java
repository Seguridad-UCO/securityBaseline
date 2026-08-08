package co.edu.uco.seguridad.pdp.recursos.infrastructure.adapter.secondary.audit;

import co.edu.uco.seguridad.pdp.recursos.domain.event.ProtectedResourceRegistered;
import org.springframework.context.event.EventListener;

import java.time.Instant;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Adaptador secundario (driven) de auditoría ficticio sustituible (criterio 07).
 *
 * <p>No implementa ningún puerto de aplicación: el caso de uso ya no la conoce. Escucha
 * {@link ProtectedResourceRegistered} vía {@code @EventListener} de Spring — síncrono, en el mismo
 * proceso — en vez de que el caso de uso la invoque directamente (ADR-0002). Registra quién registró
 * qué y cuándo — nunca la carga completa, por lo que la evidencia de auditoría no puede convertirse
 * en una segunda copia de datos que la plataforma está destinada a proteger.</p>
 *
 * <p>No es {@code @ApplicationModuleListener} (Spring Modulith) a propósito: esa anotación entrega
 * de forma asíncrona tras el commit de una transacción real, respaldada por un Event Publication
 * Registry persistente — y ningún backend de ese registro (JDBC/JPA/MongoDB/Neo4j) encaja mientras
 * la persistencia siga siendo dummy. Adoptarla ahora habría exigido una base de datos desechable
 * solo para el registro de eventos. Este es el límite honesto de ADR-0002 en esta etapa; se
 * reconsidera junto con la persistencia real (ADR-0004).</p>
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
