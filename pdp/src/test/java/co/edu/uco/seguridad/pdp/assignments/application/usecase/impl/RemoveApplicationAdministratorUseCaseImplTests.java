package co.edu.uco.seguridad.pdp.assignments.application.usecase.impl;

import co.edu.uco.seguridad.pdp.assignments.application.primaryport.request.RemoveApplicationAdministratorRequest;
import co.edu.uco.seguridad.pdp.assignments.application.primaryport.request.RevokeAssignmentRequest;
import co.edu.uco.seguridad.pdp.assignments.application.secondaryport.repository.AssignmentRepository;
import co.edu.uco.seguridad.pdp.assignments.application.usecase.RevokeAssignmentUseCase;
import co.edu.uco.seguridad.pdp.assignments.domain.Assignment;
import co.edu.uco.seguridad.pdp.assignments.domain.AssignmentCriteria;
import co.edu.uco.seguridad.pdp.assignments.domain.exception.CannotRemoveLastAdministratorException;
import co.edu.uco.seguridad.pdp.assignments.domain.model.AssignmentId;
import co.edu.uco.seguridad.pdp.assignments.domain.rule.LastAdministratorMustNotBeRevokedRule;
import co.edu.uco.seguridad.pdp.assignments.domain.rule.model.AdministratorRevocationEligibility;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.PageWindow;
import co.edu.uco.seguridad.pdp.commons.model.ResultPage;
import co.edu.uco.seguridad.pdp.commons.model.RoleId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.commons.model.UserId;
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
 * Contrato de R2 (PLAN-HU-020 §3): resuelve el conteo de administradores activos vía el repositorio,
 * y solo delega en {@code RevokeAssignmentUseCase} si {@code LastAdministratorMustNotBeRevokedRule}
 * no rechaza.
 *
 * <p>HU-022 (PLAN-HU-022.md §9): "un caso equivalente" a {@code RevokeAssignmentUseCaseImplTests} —
 * tras revocar, {@code TokenRevocationPort.revokeAllSince} captura el {@code userId} correcto.</p>
 */
class RemoveApplicationAdministratorUseCaseImplTests {

    private static final TenantId TENANT = new TenantId("universidad-uco");
    private static final ApplicationId APPLICATION = new ApplicationId(UUID.randomUUID());
    private static final RoleId ADMIN_ROLE = new RoleId(UUID.randomUUID());
    private static final UserId TARGET_USER = new UserId(UUID.randomUUID());
    private static final Instant NOW = Instant.parse("2026-09-15T00:00:00Z");

    @Test
    void revokes_when_there_is_more_than_one_active_administrator() {
        AssignmentId targetAssignmentId = new AssignmentId(UUID.randomUUID());
        Assignment targetAssignment = Assignment.assign(targetAssignmentId, TARGET_USER, TENANT, APPLICATION, ADMIN_ROLE,
                Instant.parse("2026-09-01T00:00:00Z"));
        Assignment otherAssignment = Assignment.assign(new AssignmentId(UUID.randomUUID()), new UserId(UUID.randomUUID()),
                TENANT, APPLICATION, ADMIN_ROLE, Instant.parse("2026-09-01T00:00:00Z"));
        List<RevokeAssignmentRequest> revoked = new ArrayList<>();
        RemoveApplicationAdministratorUseCaseImpl useCase = new RemoveApplicationAdministratorUseCaseImpl(
                query -> Mono.just(ADMIN_ROLE), repositoryReturning(List.of(targetAssignment, otherAssignment)),
                acceptingRule(), revokeCapturing(revoked), () -> NOW, noOpRevocation());

        StepVerifier.create(useCase.execute(new RemoveApplicationAdministratorRequest(TENANT, APPLICATION, TARGET_USER)))
                .verifyComplete();

        assertThat(revoked).hasSize(1);
        assertThat(revoked.get(0).assignmentId()).isEqualTo(targetAssignmentId);
        assertThat(revoked.get(0).tenantId()).isEqualTo(TENANT);
    }

    @Test
    void rejects_when_it_is_the_only_active_administrator_and_never_revokes() {
        AssignmentId targetAssignmentId = new AssignmentId(UUID.randomUUID());
        Assignment targetAssignment = Assignment.assign(targetAssignmentId, TARGET_USER, TENANT, APPLICATION, ADMIN_ROLE,
                Instant.parse("2026-09-01T00:00:00Z"));
        RemoveApplicationAdministratorUseCaseImpl useCase = new RemoveApplicationAdministratorUseCaseImpl(
                query -> Mono.just(ADMIN_ROLE), repositoryReturning(List.of(targetAssignment)), rejectingRule(),
                unreachableRevoke(), () -> NOW, unreachableRevocation());

        StepVerifier.create(useCase.execute(new RemoveApplicationAdministratorRequest(TENANT, APPLICATION, TARGET_USER)))
                .expectError(CannotRemoveLastAdministratorException.class)
                .verify();
    }

    @Test
    void revokes_the_subjects_tokens_since_now_after_revoking() {
        AssignmentId targetAssignmentId = new AssignmentId(UUID.randomUUID());
        Assignment targetAssignment = Assignment.assign(targetAssignmentId, TARGET_USER, TENANT, APPLICATION, ADMIN_ROLE,
                Instant.parse("2026-09-01T00:00:00Z"));
        List<UserId> revokedSubjects = new ArrayList<>();
        RemoveApplicationAdministratorUseCaseImpl useCase = new RemoveApplicationAdministratorUseCaseImpl(
                query -> Mono.just(ADMIN_ROLE), repositoryReturning(List.of(targetAssignment)), acceptingRule(),
                revokeCapturing(new ArrayList<>()), () -> NOW, revocationCapturing(revokedSubjects));

        StepVerifier.create(useCase.execute(new RemoveApplicationAdministratorRequest(TENANT, APPLICATION, TARGET_USER)))
                .verifyComplete();

        assertThat(revokedSubjects).containsExactly(TARGET_USER);
    }

    private static LastAdministratorMustNotBeRevokedRule acceptingRule() {
        return input -> {
        };
    }

    private static LastAdministratorMustNotBeRevokedRule rejectingRule() {
        return input -> {
            throw new CannotRemoveLastAdministratorException(input.applicationId());
        };
    }

    private static RevokeAssignmentUseCase revokeCapturing(List<RevokeAssignmentRequest> revoked) {
        return request -> {
            revoked.add(request);
            return Mono.empty();
        };
    }

    private static RevokeAssignmentUseCase unreachableRevoke() {
        return request -> {
            throw new AssertionError("must not revoke when it is the only active administrator");
        };
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
}
