package co.edu.uco.seguridad.shared.cache;

import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.RoleId;
import co.edu.uco.seguridad.pdp.commons.model.UserId;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import io.micrometer.core.instrument.MeterRegistry;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.Set;

/**
 * Proyecta el resultado de {@link DistributedCachePort} a telemetría (HU-023) — mismo patrón que
 * {@code ObservedAccessAuditRepository}: decora, delega, y solo después registra la señal. Un
 * contador Micrometer {@code pdp.cache.active_roles} con la etiqueta {@code outcome} en
 * {@code hit}/{@code miss}/{@code evict}.
 */
public final class ObservedDistributedCachePort implements DistributedCachePort {

    private static final String METRIC_NAME = "pdp.cache.active_roles";

    private final DistributedCachePort delegate;
    private final MeterRegistry metrics;

    public ObservedDistributedCachePort(DistributedCachePort delegate, MeterRegistry metrics) {
        this.delegate = Objects.requireNonNull(delegate, RequiredArgumentMessages.DISTRIBUTED_CACHE_PORT_DELEGATE);
        this.metrics = Objects.requireNonNull(metrics, RequiredArgumentMessages.METER_REGISTRY);
    }

    @Override
    public Mono<Set<RoleId>> get(UserId subject, ApplicationId applicationId) {
        return delegate.get(subject, applicationId)
                .doOnNext(ignored -> record("hit"))
                .switchIfEmpty(Mono.defer(() -> {
                    record("miss");
                    return Mono.empty();
                }));
    }

    @Override
    public Mono<Void> put(UserId subject, ApplicationId applicationId, Set<RoleId> roleIds) {
        return delegate.put(subject, applicationId, roleIds);
    }

    @Override
    public Mono<Void> evict(UserId subject, ApplicationId applicationId) {
        return delegate.evict(subject, applicationId).doOnSuccess(ignored -> record("evict"));
    }

    private void record(String outcome) {
        metrics.counter(METRIC_NAME, "outcome", outcome).increment();
    }
}
