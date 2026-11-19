package co.edu.uco.seguridad.pdp.assignments.application.usecase.impl;

import co.edu.uco.seguridad.pdp.assignments.application.primaryport.request.RevokeAssignmentRequest;
import co.edu.uco.seguridad.pdp.assignments.application.primaryport.request.RevokeProfileAssignmentRequest;
import co.edu.uco.seguridad.pdp.assignments.application.rule.validator.RevokeProfileAssignmentRulesValidator;
import co.edu.uco.seguridad.pdp.assignments.application.secondaryport.repository.ProfileAssignmentRepository;
import co.edu.uco.seguridad.pdp.assignments.application.usecase.RevokeAssignmentUseCase;
import co.edu.uco.seguridad.pdp.assignments.domain.ProfileAssignment;
import co.edu.uco.seguridad.pdp.assignments.domain.ProfileAssignmentCriteria;
import co.edu.uco.seguridad.pdp.assignments.domain.model.AssignmentId;
import co.edu.uco.seguridad.pdp.assignments.domain.model.ProfileAssignmentId;
import co.edu.uco.seguridad.pdp.commons.model.*;
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
 * Espeja el presupuesto de RevokeAssignmentUseCaseImplTests.
 */
class RevokeProfileAssignmentUseCaseImplTests {

    private static final TenantId TENANT = new TenantId("universidad-uco");
    private static final ProfileAssignmentId PROFILE_ASSIGNMENT = new ProfileAssignmentId(UUID.randomUUID());
    private static final Instant GRANTED_AT = Instant.parse("2026-09-12T00:00:00Z");
    private static final Instant NOW = GRANTED_AT.plusSeconds(60);

    @Test
    void revokes_every_generated_assignment_and_then_the_profile_assignment_itself() {
        AssignmentId generatedA = new AssignmentId(UUID.randomUUID());
        AssignmentId generatedB = new AssignmentId(UUID.randomUUID());
        ProfileAssignment active = ProfileAssignment.grant(PROFILE_ASSIGNMENT, new UserId(UUID.randomUUID()), TENANT,
                new ApplicationId(UUID.randomUUID()), new ProfileId(UUID.randomUUID()),
                Set.of(generatedA, generatedB), GRANTED_AT);

        List<RevokeAssignmentRequest> received = new ArrayList<>();
        List<ProfileAssignment> saved = new ArrayList<>();
        RevokeAssignmentUseCase revokeAssignmentUseCase = request -> {
            received.add(request);
            return Mono.empty();
        };
        RevokeProfileAssignmentUseCaseImpl useCase = new RevokeProfileAssignmentUseCaseImpl(
                request -> Mono.just(active), revokeAssignmentUseCase, fakeRepository(saved), () -> NOW);

        StepVerifier.create(useCase.execute(new RevokeProfileAssignmentRequest(PROFILE_ASSIGNMENT, TENANT)))
                .verifyComplete();

        assertThat(received).hasSize(2);
        received.forEach(request -> assertThat(request.tenantId()).isEqualTo(TENANT));
        assertThat(received).extracting(RevokeAssignmentRequest::assignmentId)
                .containsExactlyInAnyOrder(generatedA, generatedB);
        assertThat(saved).hasSize(1);
        assertThat(saved.get(0).isActive(NOW)).isFalse();
    }

    @Test
    void never_reaches_revoke_assignment_use_case_when_the_rules_validator_rejects_the_request() {
        RevokeProfileAssignmentRulesValidator alwaysRejects = request -> Mono.error(new RuntimeException(
                "asignación de perfil inexistente"));
        RevokeProfileAssignmentUseCaseImpl useCase = new RevokeProfileAssignmentUseCaseImpl(alwaysRejects,
                request -> {
                    throw new AssertionError("must not reach RevokeAssignmentUseCase");
                },
                unreachableRepository(), () -> NOW);

        StepVerifier.create(useCase.execute(new RevokeProfileAssignmentRequest(PROFILE_ASSIGNMENT, TENANT)))
                .expectErrorMessage("asignación de perfil inexistente")
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
