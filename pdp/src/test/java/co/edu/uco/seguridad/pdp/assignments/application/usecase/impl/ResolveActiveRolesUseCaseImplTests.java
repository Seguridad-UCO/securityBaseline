package co.edu.uco.seguridad.pdp.assignments.application.usecase.impl;

import co.edu.uco.seguridad.pdp.assignments.application.primaryport.request.ResolveActiveRolesRequest;
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
 * HU-023 (PLAN-HU-023.md §9): {@code execute} debe intentar {@code DistributedCachePort.get}
 * primero; en el miss, consultar {@code AssignmentRepository} como hasta ahora y poblar la caché
 * con {@code put}.
 */
class ResolveActiveRolesUseCaseImplTests {

    private static final UserId USER = new UserId(UUID.randomUUID());
    private static final ApplicationId APPLICATION = new ApplicationId(UUID.randomUUID());
    private static final RoleId ROLE = new RoleId(UUID.randomUUID());
    private static final Instant NOW = Instant.parse("2026-09-12T00:00:00Z");

    @Test
    void returns_the_active_role_ids_for_the_user_and_application() {
        ResolveActiveRolesUseCaseImpl useCase = new ResolveActiveRolesUseCaseImpl(
                repositoryReturning(Set.of(ROLE)), () -> NOW, emptyCache());

        StepVerifier.create(useCase.execute(new ResolveActiveRolesRequest(USER, APPLICATION)))
                .assertNext(response -> {
                    assertThat(response.userId()).isEqualTo(USER);
                    assertThat(response.applicationId()).isEqualTo(APPLICATION);
                    assertThat(response.roleIds()).containsExactly(ROLE);
                })
                .verifyComplete();
    }

    @Test
    void returns_an_empty_set_when_the_subject_has_no_assignments() {
        ResolveActiveRolesUseCaseImpl useCase = new ResolveActiveRolesUseCaseImpl(
                repositoryReturning(Set.of()), () -> NOW, emptyCache());

        StepVerifier.create(useCase.execute(new ResolveActiveRolesRequest(USER, APPLICATION)))
                .assertNext(response -> assertThat(response.roleIds()).isEmpty())
                .verifyComplete();
    }

    @Test
    void serves_from_the_cache_without_querying_the_repository_on_a_hit() {
        ResolveActiveRolesUseCaseImpl useCase = new ResolveActiveRolesUseCaseImpl(
                unreachableRepository(), () -> NOW, cacheReturning(Set.of(ROLE)));

        StepVerifier.create(useCase.execute(new ResolveActiveRolesRequest(USER, APPLICATION)))
                .assertNext(response -> assertThat(response.roleIds()).containsExactly(ROLE))
                .verifyComplete();
    }

    @Test
    void populates_the_cache_after_querying_the_repository_on_a_miss() {
        List<Object[]> put = new ArrayList<>();
        ResolveActiveRolesUseCaseImpl useCase = new ResolveActiveRolesUseCaseImpl(
                repositoryReturning(Set.of(ROLE)), () -> NOW, cacheCapturingPut(put));

        StepVerifier.create(useCase.execute(new ResolveActiveRolesRequest(USER, APPLICATION)))
                .assertNext(response -> assertThat(response.roleIds()).containsExactly(ROLE))
                .verifyComplete();

        assertThat(put).hasSize(1);
        assertThat(put.get(0)).containsExactly(USER, APPLICATION, Set.of(ROLE));
    }

    private static AssignmentRepository repositoryReturning(Set<RoleId> roleIds) {
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
                return Mono.just(roleIds);
            }

            @Override
            public Mono<Assignment> save(Assignment assignment) {
                throw new UnsupportedOperationException();
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
                throw new AssertionError("must not query the repository on a cache hit");
            }

            @Override
            public Mono<Assignment> save(Assignment assignment) {
                throw new UnsupportedOperationException();
            }
        };
    }

    private static DistributedCachePort emptyCache() {
        return new DistributedCachePort() {
            @Override
            public Mono<Set<RoleId>> get(UserId subject, ApplicationId applicationId) {
                return Mono.empty();
            }

            @Override
            public Mono<Void> put(UserId subject, ApplicationId applicationId, Set<RoleId> roleIds) {
                return Mono.empty();
            }

            @Override
            public Mono<Void> evict(UserId subject, ApplicationId applicationId) {
                throw new UnsupportedOperationException();
            }
        };
    }

    private static DistributedCachePort cacheReturning(Set<RoleId> roleIds) {
        return new DistributedCachePort() {
            @Override
            public Mono<Set<RoleId>> get(UserId subject, ApplicationId applicationId) {
                return Mono.just(roleIds);
            }

            @Override
            public Mono<Void> put(UserId subject, ApplicationId applicationId, Set<RoleId> roleIds2) {
                throw new AssertionError("must not populate the cache on a hit");
            }

            @Override
            public Mono<Void> evict(UserId subject, ApplicationId applicationId) {
                throw new UnsupportedOperationException();
            }
        };
    }

    private static DistributedCachePort cacheCapturingPut(List<Object[]> put) {
        return new DistributedCachePort() {
            @Override
            public Mono<Set<RoleId>> get(UserId subject, ApplicationId applicationId) {
                return Mono.empty();
            }

            @Override
            public Mono<Void> put(UserId subject, ApplicationId applicationId, Set<RoleId> roleIds) {
                put.add(new Object[]{subject, applicationId, roleIds});
                return Mono.empty();
            }

            @Override
            public Mono<Void> evict(UserId subject, ApplicationId applicationId) {
                throw new UnsupportedOperationException();
            }
        };
    }
}
