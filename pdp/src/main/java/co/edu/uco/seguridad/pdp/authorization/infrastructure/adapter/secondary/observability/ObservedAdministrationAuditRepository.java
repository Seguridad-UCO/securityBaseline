package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.secondary.observability;

import co.edu.uco.seguridad.shared.audit.AdministrationAuditRepository;
import co.edu.uco.seguridad.shared.audit.AdministrationEvent;
import io.micrometer.core.instrument.MeterRegistry;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * Decora {@link AdministrationAuditRepository} con telemetría (HU-021), misma forma que
 * {@code ObservedAccessAuditRepository}. Pendiente: incrementar {@code security.administration.events}
 * con {@code operation}/{@code outcome} como labels y emitir el log estructurado tras el guardado
 * real — esqueleto dejado por {@code 2-tester-spec}.
 */
public final class ObservedAdministrationAuditRepository implements AdministrationAuditRepository {

    private final AdministrationAuditRepository delegate;
    private final MeterRegistry metrics;

    public ObservedAdministrationAuditRepository(AdministrationAuditRepository delegate, MeterRegistry metrics) {
        this.delegate = Objects.requireNonNull(delegate);
        this.metrics = Objects.requireNonNull(metrics);
    }

    @Override
    public Mono<Void> save(AdministrationEvent event) {
        throw new UnsupportedOperationException("pendiente: HU-021");
    }

    @Override
    public Flux<AdministrationEvent> findByCorrelationId(String correlationId) {
        throw new UnsupportedOperationException("pendiente: HU-021");
    }
}
