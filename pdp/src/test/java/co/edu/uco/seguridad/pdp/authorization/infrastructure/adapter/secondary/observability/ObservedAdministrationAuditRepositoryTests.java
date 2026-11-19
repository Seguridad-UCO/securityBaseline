package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.secondary.observability;

import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.shared.audit.AdministrationAuditRepository;
import co.edu.uco.seguridad.shared.audit.AdministrationEvent;
import co.edu.uco.seguridad.shared.audit.AdministrationOperation;
import co.edu.uco.seguridad.shared.audit.AdministrationOutcome;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class ObservedAdministrationAuditRepositoryTests {

    @Test
    void emits_the_operational_signal_only_after_the_audit_is_persisted() {
        var metrics = new SimpleMeterRegistry();
        var saved = new AtomicReference<AdministrationEvent>();
        AdministrationEvent event = event(AdministrationOutcome.ALLOWED);

        StepVerifier.create(new ObservedAdministrationAuditRepository(durable(saved), metrics).save(event))
                .verifyComplete();

        assertThat(saved.get()).isEqualTo(event);
        assertThat(metrics.get("security.administration.events")
                .tags("operation", "ROLE_DEFINED", "outcome", "ALLOWED").counter().count()).isEqualTo(1);
        assertThat(metrics.find("security.administration.audit.failures").counter()).isNull();
    }

    @Test
    void labels_a_denied_operation_with_its_own_outcome() {
        var metrics = new SimpleMeterRegistry();

        StepVerifier.create(new ObservedAdministrationAuditRepository(durable(new AtomicReference<>()), metrics)
                        .save(event(AdministrationOutcome.DENIED)))
                .verifyComplete();

        assertThat(metrics.get("security.administration.events")
                .tags("operation", "ROLE_DEFINED", "outcome", "DENIED").counter().count()).isEqualTo(1);
    }

    @Test
    void a_failed_save_propagates_the_error_counts_the_failure_and_never_counts_an_event() {
        var metrics = new SimpleMeterRegistry();
        var failure = new IllegalStateException("surrealdb no disponible");
        AdministrationAuditRepository failing = new AdministrationAuditRepository() {
            @Override
            public Mono<Void> save(AdministrationEvent event) {
                return Mono.error(failure);
            }

            @Override
            public Flux<AdministrationEvent> findByCorrelationId(String correlationId) {
                return Flux.empty();
            }
        };

        StepVerifier.create(new ObservedAdministrationAuditRepository(failing, metrics)
                        .save(event(AdministrationOutcome.ALLOWED)))
                .expectErrorMatches(error -> error == failure)
                .verify();

        assertThat(metrics.get("security.administration.audit.failures")
                .tags("operation", "ROLE_DEFINED").counter().count()).isEqualTo(1);
        assertThat(metrics.find("security.administration.events").counter()).isNull();
    }

    @Test
    void finding_by_correlation_id_delegates_without_telemetry() {
        var metrics = new SimpleMeterRegistry();
        AdministrationEvent event = event(AdministrationOutcome.ALLOWED);
        AdministrationAuditRepository durable = new AdministrationAuditRepository() {
            @Override
            public Mono<Void> save(AdministrationEvent ignored) {
                return Mono.empty();
            }

            @Override
            public Flux<AdministrationEvent> findByCorrelationId(String correlationId) {
                return Flux.just(event);
            }
        };

        StepVerifier.create(new ObservedAdministrationAuditRepository(durable, metrics)
                        .findByCorrelationId(event.correlationId()))
                .expectNext(event)
                .verifyComplete();

        assertThat(metrics.getMeters()).isEmpty();
    }

    private static AdministrationAuditRepository durable(AtomicReference<AdministrationEvent> saved) {
        return new AdministrationAuditRepository() {
            @Override
            public Mono<Void> save(AdministrationEvent event) {
                return Mono.fromRunnable(() -> saved.set(event));
            }

            @Override
            public Flux<AdministrationEvent> findByCorrelationId(String correlationId) {
                return Flux.empty();
            }
        };
    }

    private static AdministrationEvent event(AdministrationOutcome outcome) {
        return new AdministrationEvent(UUID.randomUUID(), "correlation-1", new TenantId("universidad-uco"),
                new ApplicationId(UUID.randomUUID()), "subject-1", AdministrationOperation.ROLE_DEFINED, outcome,
                Instant.parse("2026-11-01T00:00:00Z"));
    }
}
