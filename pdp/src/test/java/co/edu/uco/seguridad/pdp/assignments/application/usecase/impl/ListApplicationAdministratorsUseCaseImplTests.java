package co.edu.uco.seguridad.pdp.assignments.application.usecase.impl;

import co.edu.uco.seguridad.pdp.assignments.application.primaryport.request.ListApplicationAdministratorsRequest;
import co.edu.uco.seguridad.pdp.assignments.application.primaryport.response.AssignmentResponse;
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
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** El caso de uso devuelve lo que el repositorio resuelve para el rol ADMIN de la aplicación (HU-020). */
class ListApplicationAdministratorsUseCaseImplTests {

    private static final TenantId TENANT = new TenantId("universidad-uco");
    private static final ApplicationId APPLICATION = new ApplicationId(UUID.randomUUID());
    private static final RoleId ADMIN_ROLE = new RoleId(UUID.randomUUID());
    private static final Instant NOW = Instant.parse("2026-09-15T00:00:00Z");

    @Test
    void returns_the_administrators_the_repository_resolves() {
        Assignment first = Assignment.assign(new AssignmentId(UUID.randomUUID()), new UserId(UUID.randomUUID()), TENANT,
                APPLICATION, ADMIN_ROLE, Instant.parse("2026-09-01T00:00:00Z"));
        Assignment second = Assignment.assign(new AssignmentId(UUID.randomUUID()), new UserId(UUID.randomUUID()), TENANT,
                APPLICATION, ADMIN_ROLE, Instant.parse("2026-09-02T00:00:00Z"));
        ListApplicationAdministratorsUseCaseImpl useCase = new ListApplicationAdministratorsUseCaseImpl(
                query -> Mono.just(ADMIN_ROLE), repositoryReturning(List.of(first, second)), () -> NOW);

        StepVerifier.create(useCase.execute(new ListApplicationAdministratorsRequest(TENANT, APPLICATION)))
                .assertNext(administrators -> {
                    List<UserId> userIds = administrators.stream().map(AssignmentResponse::userId).toList();
                    assertThat(userIds).containsExactlyInAnyOrder(first.userId(), second.userId());
                })
                .verifyComplete();
    }

    private static AssignmentRepository repositoryReturning(List<Assignment> administrators) {
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
                return Mono.just(ResultPage.of(administrators, administrators.size(), window));
            }

            @Override
            public Mono<Set<RoleId>> findActiveRoleIdsFor(UserId userId, ApplicationId applicationId, Instant now) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Mono<Assignment> save(Assignment assignment) {
                throw new UnsupportedOperationException();
            }
        };
    }
}
