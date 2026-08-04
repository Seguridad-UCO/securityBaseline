package co.edu.uco.seguridad.applications.infrastructure.audit.dummy;

import co.edu.uco.seguridad.applications.application.port.out.AuditPort;
import co.edu.uco.seguridad.applications.domain.ProtectedApplication;
import reactor.core.publisher.Mono;
import java.util.concurrent.CopyOnWriteArrayList;

public final class InMemoryAuditAdapter implements AuditPort {
    private final CopyOnWriteArrayList<String> events = new CopyOnWriteArrayList<>();
    public Mono<Void> registered(ProtectedApplication application) {
        return Mono.fromRunnable(() -> events.add("PROTECTED_APPLICATION_REGISTERED:" + application.id().value()));
    }
    public int eventCount() { return events.size(); }
}
