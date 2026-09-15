package co.edu.uco.seguridad.pdp.assignments.application.usecase.impl;

import co.edu.uco.seguridad.pdp.assignments.application.primaryport.request.AssignProfileRequest;
import co.edu.uco.seguridad.pdp.assignments.application.primaryport.request.AssignRoleRequest;
import co.edu.uco.seguridad.pdp.assignments.application.primaryport.response.AssignmentResponse;
import co.edu.uco.seguridad.pdp.assignments.application.primaryport.response.ProfileAssignmentResponse;
import co.edu.uco.seguridad.pdp.assignments.application.rule.validator.AssignProfileRulesValidator;
import co.edu.uco.seguridad.pdp.assignments.application.secondaryport.repository.ProfileAssignmentRepository;
import co.edu.uco.seguridad.pdp.assignments.application.usecase.AssignRoleUseCase;
import co.edu.uco.seguridad.pdp.assignments.domain.ProfileAssignment;
import co.edu.uco.seguridad.pdp.assignments.domain.ProfileAssignmentCriteria;
import co.edu.uco.seguridad.pdp.assignments.domain.model.AssignmentId;
import co.edu.uco.seguridad.pdp.assignments.domain.model.ProfileAssignmentId;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.PageWindow;
import co.edu.uco.seguridad.pdp.commons.model.ProfileId;
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

/** Espeja el presupuesto de AssignRoleUseCaseImplTests. */
class AssignProfileUseCaseImplTests {

    private static final TenantId TENANT = new TenantId("universidad-uco");
    private static final UserId USER = new UserId(UUID.randomUUID());
    private static final ApplicationId APPLICATION = new ApplicationId(UUID.randomUUID());
    private static final ProfileId PROFILE = new ProfileId(UUID.randomUUID());
    private static final Instant NOW = Instant.parse("2026-09-12T00:00:00Z");

    @Test
    void materializes_an_assignment_per_role_of_the_profile() {
        RoleId roleA = new RoleId(UUID.randomUUID());
        RoleId roleB = new RoleId(UUID.randomUUID());
        AssignmentId generatedA = new AssignmentId(UUID.randomUUID());
        AssignmentId generatedB = new AssignmentId(UUID.randomUUID());
        List<AssignRoleRequest> received = new ArrayList<>();
        List<ProfileAssignment> saved = new ArrayList<>();

        AssignRoleUseCase assignRoleUseCase = request -> {
            received.add(request);
            AssignmentId generated = request.roleId().equals(roleA) ? generatedA : generatedB;
            return Mono.just(new AssignmentResponse(generated, request.userId(), request.tenantId(),
                    request.applicationId(), request.roleId(), NOW, java.util.Optional.empty()));
        };
        AssignProfileUseCaseImpl useCase = new AssignProfileUseCaseImpl(
                request -> Mono.just(Set.of(roleA, roleB)), assignRoleUseCase,
                fakeRepository(saved), UUID::randomUUID, () -> NOW);

        StepVerifier.create(useCase.execute(new AssignProfileRequest(TENANT, USER, APPLICATION, PROFILE)))
                .assertNext(response -> {
                    assertThat(response.userId()).isEqualTo(USER);
                    assertThat(response.tenantId()).isEqualTo(TENANT);
                    assertThat(response.applicationId()).isEqualTo(APPLICATION);
                    assertThat(response.profileId()).isEqualTo(PROFILE);
                    assertThat(response.generatedAssignmentIds()).containsExactlyInAnyOrder(generatedA, generatedB);
                })
                .verifyComplete();

        assertThat(received).hasSize(2);
        received.forEach(request -> {
            assertThat(request.tenantId()).isEqualTo(TENANT);
            assertThat(request.userId()).isEqualTo(USER);
            assertThat(request.applicationId()).isEqualTo(APPLICATION);
        });
        assertThat(received).extracting(AssignRoleRequest::roleId).containsExactlyInAnyOrder(roleA, roleB);
        assertThat(saved).hasSize(1);
        assertThat(saved.get(0).generatedAssignmentIds()).containsExactlyInAnyOrder(generatedA, generatedB);
    }

    @Test
    void never_reaches_assign_role_use_case_when_the_rules_validator_rejects_the_request() {
        AssignProfileRulesValidator alwaysRejects = request -> Mono.error(new RuntimeException("perfil inexistente"));
        AssignProfileUseCaseImpl useCase = new AssignProfileUseCaseImpl(alwaysRejects,
                request -> { throw new AssertionError("must not reach AssignRoleUseCase"); },
                unreachableRepository(), UUID::randomUUID, () -> NOW);

        StepVerifier.create(useCase.execute(new AssignProfileRequest(TENANT, USER, APPLICATION, PROFILE)))
                .expectErrorMessage("perfil inexistente")
                .verify();
    }

    private static ProfileAssignmentRepository fakeRepository(List<ProfileAssignment> saved) {
        return new ProfileAssignmentRepository() {
            @Override
            public Mono<Boolean> existsActiveByUserApplicationProfile(UserId userId, ApplicationId applicationId,
                    ProfileId profileId, Instant now) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Mono<ProfileAssignment> findByIdForTenant(ProfileAssignmentId profileAssignmentId, TenantId tenantId) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Mono<ResultPage<ProfileAssignment>> findBy(ProfileAssignmentCriteria criteria, PageWindow window) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Mono<ProfileAssignment> save(ProfileAssignment profileAssignment) {
                saved.add(profileAssignment);
                return Mono.just(profileAssignment);
            }
        };
    }

    private static ProfileAssignmentRepository unreachableRepository() {
        return new ProfileAssignmentRepository() {
            @Override
            public Mono<Boolean> existsActiveByUserApplicationProfile(UserId userId, ApplicationId applicationId,
                    ProfileId profileId, Instant now) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Mono<ProfileAssignment> findByIdForTenant(ProfileAssignmentId profileAssignmentId, TenantId tenantId) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Mono<ResultPage<ProfileAssignment>> findBy(ProfileAssignmentCriteria criteria, PageWindow window) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Mono<ProfileAssignment> save(ProfileAssignment profileAssignment) {
                throw new AssertionError("must not save when the rules validator rejected the request");
            }
        };
    }
}
