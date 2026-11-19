package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.secondary.observability;

import co.edu.uco.seguridad.pdp.authorization.application.secondaryport.AccessAuditRepository;
import co.edu.uco.seguridad.pdp.authorization.domain.event.AccessEvent;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * Proyecta la evidencia ya persistida a telemetría. No altera la decisión ni el registro de
 * auditoría: solo emite una señal después de que el adaptador durable confirmó el guardado.
 */
public final class ObservedAccessAuditRepository implements AccessAuditRepository {

    private static final Logger LOG = LoggerFactory.getLogger(ObservedAccessAuditRepository.class);

    private final AccessAuditRepository delegate;
    private final MeterRegistry metrics;

    public ObservedAccessAuditRepository(AccessAuditRepository delegate, MeterRegistry metrics) {
        this.delegate = Objects.requireNonNull(delegate);
        this.metrics = Objects.requireNonNull(metrics);
    }

    @Override
    public Mono<Void> save(AccessEvent event) {
        return delegate.save(event).doOnSuccess(ignored -> record(event));
    }

    @Override
    public Flux<AccessEvent> findByCorrelationId(String correlationId) {
        return delegate.findByCorrelationId(correlationId);
    }

    private void record(AccessEvent event) {
        String decision = event.state().name();
        String reason = event.reasonCode().name();
        metrics.counter("security.access.events", "decision", decision, "reason", reason).increment();
        LOG.atInfo()
                .addKeyValue("event.name", "security.access.event.recorded")
                .addKeyValue("event.category", "audit")
                .addKeyValue("decisionId", event.decisionId())
                .addKeyValue("requestId", event.requestId())
                .addKeyValue("correlationId", event.correlationId())
                .addKeyValue("decision", decision)
                .addKeyValue("reason", reason)
                .addKeyValue("action", event.action().name())
                .log("Evidencia de decisión de acceso persistida");
    }
}
