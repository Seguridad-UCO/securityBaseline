package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.secondary.observability;

import co.edu.uco.seguridad.shared.audit.AdministrationAuditRepository;
import co.edu.uco.seguridad.shared.audit.AdministrationEvent;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * Decora {@link AdministrationAuditRepository} con telemetría (HU-021), misma forma que
 * {@code ObservedAccessAuditRepository}: solo emite la señal después de que el adaptador durable
 * confirmó el guardado. Además deja visible el fallo de guardado — los casos de uso lo absorben a
 * propósito (best-effort, PLAN-HU-021 §0), así que este es el único lugar donde queda rastro de él.
 */
public final class ObservedAdministrationAuditRepository implements AdministrationAuditRepository {

    private static final Logger LOG = LoggerFactory.getLogger(ObservedAdministrationAuditRepository.class);

    private final AdministrationAuditRepository delegate;
    private final MeterRegistry metrics;

    public ObservedAdministrationAuditRepository(AdministrationAuditRepository delegate, MeterRegistry metrics) {
        this.delegate = Objects.requireNonNull(delegate);
        this.metrics = Objects.requireNonNull(metrics);
    }

    @Override
    public Mono<Void> save(AdministrationEvent event) {
        return delegate.save(event)
                .doOnSuccess(ignored -> record(event))
                .doOnError(error -> recordFailure(event, error));
    }

    @Override
    public Flux<AdministrationEvent> findByCorrelationId(String correlationId) {
        return delegate.findByCorrelationId(correlationId);
    }

    private void record(AdministrationEvent event) {
        String operation = event.operation().name();
        String outcome = event.outcome().name();
        metrics.counter("security.administration.events", "operation", operation, "outcome", outcome).increment();
        LOG.atInfo()
                .addKeyValue("event.name", "security.administration.event.recorded")
                .addKeyValue("event.category", "audit")
                .addKeyValue("eventId", event.eventId())
                .addKeyValue("correlationId", event.correlationId())
                .addKeyValue("applicationId", event.applicationId().value())
                .addKeyValue("operation", operation)
                .addKeyValue("outcome", outcome)
                .log("Evidencia de operación administrativa persistida");
    }

    private void recordFailure(AdministrationEvent event, Throwable error) {
        String operation = event.operation().name();
        metrics.counter("security.administration.audit.failures", "operation", operation).increment();
        LOG.atWarn()
                .addKeyValue("event.name", "security.administration.event.failed")
                .addKeyValue("event.category", "audit")
                .addKeyValue("eventId", event.eventId())
                .addKeyValue("correlationId", event.correlationId())
                .addKeyValue("operation", operation)
                .addKeyValue("outcome", event.outcome().name())
                .addKeyValue("error.type", error.getClass().getName())
                .log("No se pudo persistir la evidencia de una operación administrativa");
    }
}
