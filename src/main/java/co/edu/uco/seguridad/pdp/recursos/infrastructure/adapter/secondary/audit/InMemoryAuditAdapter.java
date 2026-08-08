package co.edu.uco.seguridad.pdp.recursos.infrastructure.adapter.secondary.audit;

import co.edu.uco.seguridad.pdp.recursos.application.port.secondary.AuditPort;
import co.edu.uco.seguridad.pdp.recursos.domain.ProtectedResource;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Adaptador secundario (driven) de auditoría ficticio sustituible (criterio 07).
 *
 * <p>Registra quién registró qué y cuándo — nunca la carga completa, por lo que la evidencia de auditoría no puede
 * convertirse en una segunda copia de datos que la plataforma está destinada a proteger.</p>
 */
public final class InMemoryAuditAdapter implements AuditPort {

    /** Un hecho de auditoría. Solo identificadores: suficiente para reconstruir la acción, nada más. */
    public record AuditEvent(String event, String tenantId, String applicationId, String resourceId, Instant at) {
    }

    private final Queue<AuditEvent> events = new ConcurrentLinkedQueue<>();

    @Override
    public Mono<Void> protectedApplicationRegistered(ProtectedResource resource) {
        return Mono.fromRunnable(() -> events.add(new AuditEvent(
                "protected_application.registered",
                resource.tenantId().value(),
                resource.applicationId().value().toString(),
                resource.id().value().toString(),
                resource.registeredAt())));
    }

    public List<AuditEvent> recorded() {
        return List.copyOf(events);
    }
}
