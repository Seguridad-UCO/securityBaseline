package co.edu.uco.seguridad.shared.cache;

import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.RoleId;
import co.edu.uco.seguridad.pdp.commons.model.UserId;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PLAN-HU-023.md §9 — mismo patrón que {@code ObservedAccessAuditRepositoryTests}: decora, delega, y
 * solo después registra la señal (aquí un contador {@code pdp.cache.active_roles} con la etiqueta
 * {@code outcome}).
 */
class ObservedDistributedCachePortTests {

    private static final UserId SUBJECT = new UserId(UUID.randomUUID());
    private static final ApplicationId APPLICATION = new ApplicationId(UUID.randomUUID());
    private static final RoleId ROLE = new RoleId(UUID.randomUUID());

    @Test
    void records_a_hit_when_the_delegate_resolves_a_value() {
        var metrics = new SimpleMeterRegistry();
        DistributedCachePort delegate = new DistributedCachePort() {
            @Override
            public Mono<Set<RoleId>> get(UserId subject, ApplicationId applicationId) {
                return Mono.just(Set.of(ROLE));
            }

            @Override
            public Mono<Void> put(UserId subject, ApplicationId applicationId, Set<RoleId> roleIds) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Mono<Void> evict(UserId subject, ApplicationId applicationId) {
                throw new UnsupportedOperationException();
            }
        };

        StepVerifier.create(new ObservedDistributedCachePort(delegate, metrics).get(SUBJECT, APPLICATION))
                .assertNext(roleIds -> assertThat(roleIds).containsExactly(ROLE))
                .verifyComplete();

        assertThat(metrics.get("pdp.cache.active_roles").tags("outcome", "hit").counter().count()).isEqualTo(1);
    }

    @Test
    void records_a_miss_when_the_delegate_resolves_empty() {
        var metrics = new SimpleMeterRegistry();
        DistributedCachePort delegate = new DistributedCachePort() {
            @Override
            public Mono<Set<RoleId>> get(UserId subject, ApplicationId applicationId) {
                return Mono.empty();
            }

            @Override
            public Mono<Void> put(UserId subject, ApplicationId applicationId, Set<RoleId> roleIds) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Mono<Void> evict(UserId subject, ApplicationId applicationId) {
                throw new UnsupportedOperationException();
            }
        };

        StepVerifier.create(new ObservedDistributedCachePort(delegate, metrics).get(SUBJECT, APPLICATION)).verifyComplete();

        assertThat(metrics.get("pdp.cache.active_roles").tags("outcome", "miss").counter().count()).isEqualTo(1);
    }

    @Test
    void records_an_evict_after_delegating() {
        var metrics = new SimpleMeterRegistry();
        DistributedCachePort delegate = new DistributedCachePort() {
            @Override
            public Mono<Set<RoleId>> get(UserId subject, ApplicationId applicationId) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Mono<Void> put(UserId subject, ApplicationId applicationId, Set<RoleId> roleIds) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Mono<Void> evict(UserId subject, ApplicationId applicationId) {
                return Mono.empty();
            }
        };

        StepVerifier.create(new ObservedDistributedCachePort(delegate, metrics).evict(SUBJECT, APPLICATION)).verifyComplete();

        assertThat(metrics.get("pdp.cache.active_roles").tags("outcome", "evict").counter().count()).isEqualTo(1);
    }
}
