package co.edu.uco.seguridad.pdp.assignments.application.usecase.impl;

import co.edu.uco.seguridad.pdp.assignments.application.primaryport.request.ResolveActiveRolesRequest;
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
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ResolveActiveRolesUseCaseImplTests {

    private static final UserId USER = new UserId(UUID.randomUUID());
    private static final ApplicationId APPLICATION = new ApplicationId(UUID.randomUUID());
    private static final RoleId ROLE = new RoleId(UUID.randomUUID());
    private static final Instant NOW = Instant.parse("2026-09-12T00:00:00Z");

    @Test
    void returns_the_active_role_ids_for_the_user_and_application() {
        ResolveActiveRolesUseCaseImpl useCase = new ResolveActiveRolesUseCaseImpl(repositoryReturning(Set.of(ROLE)), () -> NOW);

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
        ResolveActiveRolesUseCaseImpl useCase = new ResolveActiveRolesUseCaseImpl(repositoryReturning(Set.of()), () -> NOW);

        StepVerifier.create(useCase.execute(new ResolveActiveRolesRequest(USER, APPLICATION)))
                .assertNext(response -> assertThat(response.roleIds()).isEmpty())
                .verifyComplete();
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
}
