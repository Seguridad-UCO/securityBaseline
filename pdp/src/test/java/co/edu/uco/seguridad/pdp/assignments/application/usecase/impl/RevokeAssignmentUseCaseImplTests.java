package co.edu.uco.seguridad.pdp.assignments.application.usecase.impl;

import co.edu.uco.seguridad.pdp.assignments.application.primaryport.request.RevokeAssignmentRequest;
import co.edu.uco.seguridad.pdp.assignments.application.rule.validator.RevokeAssignmentRulesValidator;
import co.edu.uco.seguridad.pdp.assignments.application.secondaryport.repository.AssignmentRepository;
import co.edu.uco.seguridad.pdp.assignments.domain.Assignment;
import co.edu.uco.seguridad.pdp.assignments.domain.AssignmentCriteria;
import co.edu.uco.seguridad.pdp.assignments.domain.model.AssignmentId;
import co.edu.uco.seguridad.pdp.commons.model.*;
import co.edu.uco.seguridad.shared.cache.DistributedCachePort;
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
 * <p>Revocar una asignación conserva la sesión de la persona: el PDP calcula los permisos vigentes
 * en cada decisión. Tras revocar, debe invocar
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
                request -> Mono.just(ASSIGNMENT), repositoryCapturing(saved), () -> NOW, noOpCache());

        StepVerifier.create(useCase.execute(new RevokeAssignmentRequest(ID, TENANT))).verifyComplete();

        assertThat(saved).hasSize(1);
        assertThat(saved.get(0).validity().validUntil()).contains(NOW);
    }

    @Test
    void never_saves_when_the_rules_validator_rejects_the_request() {
        RuntimeException rejection = new RuntimeException("asignación no encontrada");
        RevokeAssignmentRulesValidator alwaysRejects = request -> Mono.error(rejection);
        RevokeAssignmentUseCaseImpl useCase = new RevokeAssignmentUseCaseImpl(alwaysRejects, unreachableRepository(), () -> NOW,
                unreachableCache());

        StepVerifier.create(useCase.execute(new RevokeAssignmentRequest(ID, TENANT)))
                .expectErrorMessage("asignación no encontrada")
                .verify();
    }

    @Test
    void preserves_the_subject_session_after_revoking_an_assignment() {
        List<Assignment> saved = new ArrayList<>();
        RevokeAssignmentUseCaseImpl useCase = new RevokeAssignmentUseCaseImpl(
                request -> Mono.just(ASSIGNMENT), repositoryCapturing(saved), () -> NOW, noOpCache());

        StepVerifier.create(useCase.execute(new RevokeAssignmentRequest(ID, TENANT))).verifyComplete();

        assertThat(saved).hasSize(1);
    }

    @Test
    void evicts_the_cache_for_the_user_and_application_after_revoking() {
        List<Assignment> saved = new ArrayList<>();
        List<Object[]> evicted = new ArrayList<>();
        RevokeAssignmentUseCaseImpl useCase = new RevokeAssignmentUseCaseImpl(
                request -> Mono.just(ASSIGNMENT), repositoryCapturing(saved), () -> NOW, cacheCapturingEvict(evicted));

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
                evicted.add(new Object[]{subject, applicationId});
                return Mono.empty();
            }
        };
    }
}
