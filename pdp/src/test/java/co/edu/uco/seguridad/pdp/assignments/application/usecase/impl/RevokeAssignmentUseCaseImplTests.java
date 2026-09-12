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
 */
class RevokeAssignmentUseCaseImplTests {

    private static final AssignmentId ID = new AssignmentId(UUID.randomUUID());
    private static final TenantId TENANT = new TenantId("universidad-uco");
    private static final Assignment ASSIGNMENT = Assignment.assign(ID, new UserId(UUID.randomUUID()), TENANT,
            new ApplicationId(UUID.randomUUID()), new RoleId(UUID.randomUUID()), Instant.parse("2026-09-11T00:00:00Z"));
    private static final Instant NOW = Instant.parse("2026-09-12T00:00:00Z");

    @Test
    void saves_the_assignment_with_the_end_fixed_and_completes() {
        List<Assignment> saved = new ArrayList<>();
        RevokeAssignmentUseCaseImpl useCase = new RevokeAssignmentUseCaseImpl(
                request -> Mono.just(ASSIGNMENT), repositoryCapturing(saved), () -> NOW);

        StepVerifier.create(useCase.execute(new RevokeAssignmentRequest(ID, TENANT))).verifyComplete();

        assertThat(saved).hasSize(1);
        assertThat(saved.get(0).validity().validUntil()).contains(NOW);
    }

    @Test
    void never_saves_when_the_rules_validator_rejects_the_request() {
        RuntimeException rejection = new RuntimeException("asignación no encontrada");
        RevokeAssignmentRulesValidator alwaysRejects = request -> Mono.error(rejection);
        RevokeAssignmentUseCaseImpl useCase = new RevokeAssignmentUseCaseImpl(alwaysRejects, unreachableRepository(), () -> NOW);

        StepVerifier.create(useCase.execute(new RevokeAssignmentRequest(ID, TENANT)))
                .expectErrorMessage("asignación no encontrada")
                .verify();
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
}
