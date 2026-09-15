package co.edu.uco.seguridad.pdp.assignments.application.rule.validator;

import co.edu.uco.seguridad.pdp.assignments.application.primaryport.request.ProfileAssignmentOwnershipQuery;
import co.edu.uco.seguridad.pdp.assignments.application.rule.validator.impl.ProfileAssignmentApplicationLookupValidatorImpl;
import co.edu.uco.seguridad.pdp.assignments.application.secondaryport.repository.ProfileAssignmentRepository;
import co.edu.uco.seguridad.pdp.assignments.domain.ProfileAssignment;
import co.edu.uco.seguridad.pdp.assignments.domain.ProfileAssignmentCriteria;
import co.edu.uco.seguridad.pdp.assignments.domain.exception.ProfileAssignmentNotFoundException;
import co.edu.uco.seguridad.pdp.assignments.domain.model.ProfileAssignmentId;
import co.edu.uco.seguridad.pdp.assignments.domain.rule.impl.ProfileAssignmentMustExistForTenantRuleImpl;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.PageWindow;
import co.edu.uco.seguridad.pdp.commons.model.ProfileId;
import co.edu.uco.seguridad.pdp.commons.model.ResultPage;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.commons.model.UserId;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** HU-019: resuelve a qué aplicación pertenece una asignación de perfil, para gatear su revocación. */
class ProfileAssignmentApplicationLookupValidatorImplTests {

    private static final TenantId TENANT = new TenantId("universidad-uco");
    private static final ProfileAssignmentId PROFILE_ASSIGNMENT = new ProfileAssignmentId(UUID.randomUUID());
    private static final ApplicationId APPLICATION = new ApplicationId(UUID.randomUUID());
    private static final Instant ASSIGNED_AT = Instant.parse("2026-09-15T00:00:00Z");

    @Test
    void resolves_the_application_id_of_an_existing_profile_assignment() {
        ProfileAssignment assignment = ProfileAssignment.grant(PROFILE_ASSIGNMENT, new UserId(UUID.randomUUID()),
                TENANT, APPLICATION, new ProfileId(UUID.randomUUID()), Set.of(), ASSIGNED_AT);
        ProfileAssignmentApplicationLookupValidatorImpl validator = new ProfileAssignmentApplicationLookupValidatorImpl(
                repositoryFinding(assignment), new ProfileAssignmentMustExistForTenantRuleImpl());

        StepVerifier.create(validator.execute(new ProfileAssignmentOwnershipQuery(PROFILE_ASSIGNMENT, TENANT)))
                .assertNext(result -> assertThat(result).isEqualTo(APPLICATION))
                .verifyComplete();
    }

    @Test
    void refuses_a_profile_assignment_that_does_not_exist_for_the_tenant() {
        ProfileAssignmentApplicationLookupValidatorImpl validator = new ProfileAssignmentApplicationLookupValidatorImpl(
                repositoryFinding(null), new ProfileAssignmentMustExistForTenantRuleImpl());

        StepVerifier.create(validator.execute(new ProfileAssignmentOwnershipQuery(PROFILE_ASSIGNMENT, TENANT)))
                .expectError(ProfileAssignmentNotFoundException.class)
                .verify();
    }

    private static ProfileAssignmentRepository repositoryFinding(ProfileAssignment found) {
        return new ProfileAssignmentRepository() {
            @Override
            public Mono<Boolean> existsActiveByUserApplicationProfile(UserId userId, ApplicationId applicationId,
                    ProfileId profileId, Instant now) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Mono<ProfileAssignment> findByIdForTenant(ProfileAssignmentId profileAssignmentId, TenantId tenantId) {
                return found == null ? Mono.empty() : Mono.just(found);
            }

            @Override
            public Mono<ResultPage<ProfileAssignment>> findBy(ProfileAssignmentCriteria criteria, PageWindow window) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Mono<ProfileAssignment> save(ProfileAssignment profileAssignment) {
                throw new UnsupportedOperationException();
            }
        };
    }
}
