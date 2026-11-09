package co.edu.uco.seguridad.pdp.assignments.application.usecase.impl;

import co.edu.uco.seguridad.pdp.assignments.application.primaryport.request.AssignRoleRequest;
import co.edu.uco.seguridad.pdp.assignments.application.rule.validator.AssignRoleRulesValidator;
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
 * Con las reglas sustituidas por un dummy que siempre aprueba: aquí se prueba la construcción y la
 * persistencia, no las reglas.
 *
 * <p>HU-023 (PLAN-HU-023.md §9): tras guardar, debe invocar {@code DistributedCachePort.evict} con
 * el {@code userId}/{@code applicationId} de la respuesta.</p>
 */
class AssignRoleUseCaseImplTests {

    private static final TenantId TENANT = new TenantId("universidad-uco");
    private static final UserId USER = new UserId(UUID.randomUUID());
    private static final ApplicationId APPLICATION = new ApplicationId(UUID.randomUUID());
    private static final RoleId ROLE = new RoleId(UUID.randomUUID());
    private static final Instant NOW = Instant.parse("2026-09-12T00:00:00Z");

    @Test
    void assigns_the_role_with_a_generated_id_and_the_current_time_and_no_end() {
        List<Assignment> saved = new ArrayList<>();
        UUID fixedId = UUID.randomUUID();
        AssignRoleUseCaseImpl useCase = new AssignRoleUseCaseImpl(
                dto -> Mono.empty(), repositoryCapturing(saved), () -> fixedId, () -> NOW, noOpCache());

        StepVerifier.create(useCase.execute(new AssignRoleRequest(TENANT, USER, APPLICATION, ROLE)))
                .assertNext(response -> {
                    assertThat(response.id()).isEqualTo(new AssignmentId(fixedId));
                    assertThat(response.userId()).isEqualTo(USER);
                    assertThat(response.tenantId()).isEqualTo(TENANT);
                    assertThat(response.applicationId()).isEqualTo(APPLICATION);
                    assertThat(response.roleId()).isEqualTo(ROLE);
                    assertThat(response.validFrom()).isEqualTo(NOW);
                    assertThat(response.validUntil()).isEmpty();
                })
                .verifyComplete();

        assertThat(saved).hasSize(1);
    }

    @Test
    void never_saves_when_the_rules_validator_rejects_the_request() {
        RuntimeException rejection = new RuntimeException("usuario inexistente");
        AssignRoleRulesValidator alwaysRejects = dto -> Mono.error(rejection);
        AssignRoleUseCaseImpl useCase = new AssignRoleUseCaseImpl(
                alwaysRejects, unreachableRepository(), UUID::randomUUID, () -> NOW, unreachableCache());

        StepVerifier.create(useCase.execute(new AssignRoleRequest(TENANT, USER, APPLICATION, ROLE)))
                .expectErrorMessage("usuario inexistente")
                .verify();
    }

    @Test
    void evicts_the_cache_for_the_user_and_application_after_assigning() {
        List<Assignment> saved = new ArrayList<>();
        List<Object[]> evicted = new ArrayList<>();
        AssignRoleUseCaseImpl useCase = new AssignRoleUseCaseImpl(
                dto -> Mono.empty(), repositoryCapturing(saved), UUID::randomUUID, () -> NOW, cacheCapturingEvict(evicted));

        StepVerifier.create(useCase.execute(new AssignRoleRequest(TENANT, USER, APPLICATION, ROLE)))
                .assertNext(response -> {
                })
                .verifyComplete();

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
