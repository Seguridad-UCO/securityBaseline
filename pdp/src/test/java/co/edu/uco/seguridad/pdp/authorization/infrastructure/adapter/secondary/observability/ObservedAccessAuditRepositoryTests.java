package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.secondary.observability;

import co.edu.uco.seguridad.pdp.authorization.application.secondaryport.AccessAuditRepository;
import co.edu.uco.seguridad.pdp.authorization.domain.event.AccessEvent;
import co.edu.uco.seguridad.pdp.authorization.domain.model.DecisionState;
import co.edu.uco.seguridad.pdp.authorization.domain.model.ReasonCode;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.resources.domain.model.HttpVerb;
import co.edu.uco.seguridad.pdp.resources.domain.model.ResourcePath;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class ObservedAccessAuditRepositoryTests {

    @Test
    void emits_the_operational_signal_only_after_the_audit_is_persisted() {
        var metrics = new SimpleMeterRegistry();
        var saved = new AtomicReference<AccessEvent>();
        AccessAuditRepository durable = new AccessAuditRepository() {
            @Override
            public Mono<Void> save(AccessEvent event) {
                return Mono.fromRunnable(() -> saved.set(event));
            }

            @Override
            public Flux<AccessEvent> findByCorrelationId(String correlationId) {
                return Flux.empty();
            }
        };
        AccessEvent event = event();

        StepVerifier.create(new ObservedAccessAuditRepository(durable, metrics).save(event)).verifyComplete();

        assertThat(saved.get()).isEqualTo(event);
        assertThat(metrics.get("security.access.events").tags("decision", "ALLOW", "reason", "POLICY_ALLOWED")
                .counter().count()).isEqualTo(1);
    }

    private static AccessEvent event() {
        return new AccessEvent(UUID.randomUUID(), UUID.randomUUID(), "request-1", "correlation-1",
                new TenantId("universidad-uco"), new ApplicationId(UUID.randomUUID()), "subject-1",
                new ResourcePath("/estudiantes"), HttpVerb.GET, DecisionState.ALLOW, ReasonCode.POLICY_ALLOWED,
                Instant.parse("2026-09-13T00:00:00Z"));
    }
}
