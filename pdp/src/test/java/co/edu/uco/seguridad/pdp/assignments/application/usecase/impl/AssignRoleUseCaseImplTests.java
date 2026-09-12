package co.edu.uco.seguridad.pdp.assignments.application.usecase.impl;

import co.edu.uco.seguridad.pdp.assignments.application.primaryport.request.AssignRoleRequest;
import co.edu.uco.seguridad.pdp.assignments.application.rule.validator.AssignRoleRulesValidator;
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

/** Con las reglas sustituidas por un dummy que siempre aprueba: aquí se prueba la construcción y la persistencia, no las reglas. */
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
                dto -> Mono.empty(), repositoryCapturing(saved), () -> fixedId, () -> NOW);

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
                alwaysRejects, unreachableRepository(), UUID::randomUUID, () -> NOW);

        StepVerifier.create(useCase.execute(new AssignRoleRequest(TENANT, USER, APPLICATION, ROLE)))
                .expectErrorMessage("usuario inexistente")
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
