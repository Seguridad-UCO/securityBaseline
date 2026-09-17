package co.edu.uco.seguridad.pdp.assignments.application.usecase.impl;

import co.edu.uco.seguridad.pdp.assignments.application.primaryport.request.RevokeAssignmentRequest;
import co.edu.uco.seguridad.pdp.assignments.application.rule.validator.RevokeAssignmentRulesValidator;
import co.edu.uco.seguridad.pdp.assignments.application.secondaryport.repository.AssignmentRepository;
import co.edu.uco.seguridad.pdp.assignments.domain.Assignment;
import co.edu.uco.seguridad.pdp.assignments.domain.AssignmentCriteria;
import co.edu.uco.seguridad.pdp.assignments.domain.model.AssignmentId;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.PageWindow;
import co.edu.uco.seguridad.pdp.commons.model.ResultPage;
import co.edu.uco.seguridad.pdp.commons.model.RoleId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.commons.model.UserId;
import co.edu.uco.seguridad.shared.cache.DistributedCachePort;
import co.edu.uco.seguridad.shared.security.revocation.TokenRevocationPort;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * El validador ya encontró y validó la asignación (R5) — este caso de uso solo la transforma
 * (Assignment.revoke) y la guarda. No vuelve a consultar ni a decidir nada.
 *
 * <p>HU-022 (PLAN-HU-022.md §9): tras guardar, invoca {@code TokenRevocationPort.revokeAllSince}
 * con el {@code userId} de la asignación revocada — fail-closed también en escritura, así que si la
 * revocación falla, la operación completa falla.</p>
 *
 * <p>HU-023 (PLAN-HU-023.md §9): tras revocar, debe invocar además
 * {@code DistributedCachePort.evict} con el {@code userId}/{@code applicationId} de la asignación
 * revocada.</p>
 */
class RevokeAssignmentUseCaseImplTests {

    private static final AssignmentId ID = new AssignmentId(UUID.randomUUID());
    private static final TenantId TENANT = new TenantId("universidad-uco");
    private static final UserId USER = new UserId(UUID.randomUUID());
    private static final ApplicationId APPLICATION = new ApplicationId(UUID.randomUUID());
    private static final Assignment ASSIGNMENT = Assignment.assign(ID, USER, TENANT,
            APPLICATION, new RoleId(UUID.randomUUID()), Instant.parse("2026-09-11T00:00:00Z"));
    private static final Instant NOW = Instant.parse("2026-09-12T00:00:00Z");

    @Test
    void saves_the_assignment_with_the_end_fixed_and_completes() {
        List<Assignment> saved = new ArrayList<>();
        RevokeAssignmentUseCaseImpl useCase = new RevokeAssignmentUseCaseImpl(
                request -> Mono.just(ASSIGNMENT), repositoryCapturing(saved), () -> NOW, noOpRevocation(), noOpCache());

        StepVerifier.create(useCase.execute(new RevokeAssignmentRequest(ID, TENANT))).verifyComplete();

        assertThat(saved).hasSize(1);
        assertThat(saved.get(0).validity().validUntil()).contains(NOW);
    }

    @Test
    void never_saves_when_the_rules_validator_rejects_the_request() {
        RuntimeException rejection = new RuntimeException("asignación no encontrada");
        RevokeAssignmentRulesValidator alwaysRejects = request -> Mono.error(rejection);
        RevokeAssignmentUseCaseImpl useCase = new RevokeAssignmentUseCaseImpl(alwaysRejects, unreachableRepository(), () -> NOW,
                unreachableRevocation(), unreachableCache());

        StepVerifier.create(useCase.execute(new RevokeAssignmentRequest(ID, TENANT)))
                .expectErrorMessage("asignación no encontrada")
                .verify();
    }

    @Test
    void revokes_the_subjects_tokens_since_now_after_saving() {
        List<Assignment> saved = new ArrayList<>();
        List<UserId> revokedSubjects = new ArrayList<>();
        RevokeAssignmentUseCaseImpl useCase = new RevokeAssignmentUseCaseImpl(
                request -> Mono.just(ASSIGNMENT), repositoryCapturing(saved), () -> NOW, revocationCapturing(revokedSubjects),
                noOpCache());

        StepVerifier.create(useCase.execute(new RevokeAssignmentRequest(ID, TENANT))).verifyComplete();

        assertThat(revokedSubjects).containsExactly(USER);
    }

    @Test
    void fails_the_whole_operation_when_revocation_fails() {
        List<Assignment> saved = new ArrayList<>();
        RuntimeException redisDown = new RuntimeException("redis no disponible");
        RevokeAssignmentUseCaseImpl useCase = new RevokeAssignmentUseCaseImpl(
                request -> Mono.just(ASSIGNMENT), repositoryCapturing(saved), () -> NOW, failingRevocation(redisDown), noOpCache());

        StepVerifier.create(useCase.execute(new RevokeAssignmentRequest(ID, TENANT)))
                .expectErrorMessage("redis no disponible")
                .verify();
    }

    @Test
    void evicts_the_cache_for_the_user_and_application_after_revoking() {
        List<Assignment> saved = new ArrayList<>();
        List<Object[]> evicted = new ArrayList<>();
        RevokeAssignmentUseCaseImpl useCase = new RevokeAssignmentUseCaseImpl(
                request -> Mono.just(ASSIGNMENT), repositoryCapturing(saved), () -> NOW, noOpRevocation(),
                cacheCapturingEvict(evicted));

        StepVerifier.create(useCase.execute(new RevokeAssignmentRequest(ID, TENANT))).verifyComplete();

        assertThat(evicted).hasSize(1);
        assertThat(evicted.get(0)).containsExactly(USER, APPLICATION);
    }

    private static AssignmentRepository repositoryCapturing(List<Assignment> saved) {
        return new AssignmentRepository() {
            @Override
            public Mono<Boolean> existsActiveByUserApplicationRole(UserId userId, ApplicationId applicationId, RoleId roleId,
                    Instant now) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Mono<Assignment> findByIdForTenant(AssignmentId assignmentId, TenantId tenantId) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Mono<ResultPage<Assignment>> findBy(AssignmentCriteria criteria, PageWindow window) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Mono<Set<RoleId>> findActiveRoleIdsFor(UserId userId, ApplicationId applicationId, Instant now) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Mono<Assignment> save(Assignment assignment) {
                saved.add(assignment);
                return Mono.just(assignment);
            }
        };
    }

    private static AssignmentRepository unreachableRepository() {
        return new AssignmentRepository() {
            @Override
            public Mono<Boolean> existsActiveByUserApplicationRole(UserId userId, ApplicationId applicationId, RoleId roleId,
                    Instant now) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Mono<Assignment> findByIdForTenant(AssignmentId assignmentId, TenantId tenantId) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Mono<ResultPage<Assignment>> findBy(AssignmentCriteria criteria, PageWindow window) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Mono<Set<RoleId>> findActiveRoleIdsFor(UserId userId, ApplicationId applicationId, Instant now) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Mono<Assignment> save(Assignment assignment) {
                throw new AssertionError("must not save when the rules reject the request");
            }
        };
    }

    private static TokenRevocationPort noOpRevocation() {
        return new TokenRevocationPort() {
            @Override
            public Mono<Void> revokeAllSince(UserId subject, Instant since) {
                return Mono.empty();
            }

            @Override
            public Mono<Boolean> isRevoked(UserId subject, Instant issuedAt) {
                throw new UnsupportedOperationException();
            }
        };
    }

    private static TokenRevocationPort unreachableRevocation() {
        return new TokenRevocationPort() {
            @Override
            public Mono<Void> revokeAllSince(UserId subject, Instant since) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Mono<Boolean> isRevoked(UserId subject, Instant issuedAt) {
                throw new UnsupportedOperationException();
            }
        };
    }

    private static TokenRevocationPort revocationCapturing(List<UserId> revokedSubjects) {
        return new TokenRevocationPort() {
            @Override
            public Mono<Void> revokeAllSince(UserId subject, Instant since) {
                revokedSubjects.add(subject);
                return Mono.empty();
            }

            @Override
            public Mono<Boolean> isRevoked(UserId subject, Instant issuedAt) {
                throw new UnsupportedOperationException();
            }
        };
    }

    private static TokenRevocationPort failingRevocation(RuntimeException failure) {
        return new TokenRevocationPort() {
            @Override
            public Mono<Void> revokeAllSince(UserId subject, Instant since) {
                return Mono.error(failure);
            }

            @Override
            public Mono<Boolean> isRevoked(UserId subject, Instant issuedAt) {
                throw new UnsupportedOperationException();
            }
        };
    }

    private static DistributedCachePort noOpCache() {
        return new DistributedCachePort() {
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
    }

    private static DistributedCachePort unreachableCache() {
        return new DistributedCachePort() {
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
                throw new AssertionError("must not evict when the rules reject the request");
            }
        };
    }

    private static DistributedCachePort cacheCapturingEvict(List<Object[]> evicted) {
        return new DistributedCachePort() {
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
                evicted.add(new Object[] {subject, applicationId});
                return Mono.empty();
            }
        };
    }
}
