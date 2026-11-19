package co.edu.uco.seguridad.pdp.assignments.application.rule.validator;

import co.edu.uco.seguridad.pdp.assignments.application.primaryport.request.AssignmentOwnershipQuery;
import co.edu.uco.seguridad.pdp.assignments.application.rule.validator.impl.AssignmentApplicationLookupValidatorImpl;
import co.edu.uco.seguridad.pdp.assignments.application.secondaryport.repository.AssignmentRepository;
import co.edu.uco.seguridad.pdp.assignments.domain.Assignment;
import co.edu.uco.seguridad.pdp.assignments.domain.AssignmentCriteria;
import co.edu.uco.seguridad.pdp.assignments.domain.exception.AssignmentNotFoundException;
import co.edu.uco.seguridad.pdp.assignments.domain.model.AssignmentId;
import co.edu.uco.seguridad.pdp.assignments.domain.rule.impl.AssignmentMustExistForTenantRuleImpl;
import co.edu.uco.seguridad.pdp.commons.model.*;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * HU-018: resuelve a qué aplicación pertenece una asignación, para gatear RevokeAssignment.
 */
class AssignmentApplicationLookupValidatorImplTests {

    private static final TenantId TENANT = new TenantId("universidad-uco");
    private static final AssignmentId ASSIGNMENT = new AssignmentId(UUID.randomUUID());
    private static final ApplicationId APPLICATION = new ApplicationId(UUID.randomUUID());
    private static final Instant ASSIGNED_AT = Instant.parse("2026-09-15T00:00:00Z");

    @Test
    void resolves_the_application_id_of_an_existing_assignment() {
        Assignment assignment = Assignment.assign(ASSIGNMENT, new UserId(UUID.randomUUID()), TENANT, APPLICATION,
                new RoleId(UUID.randomUUID()), ASSIGNED_AT);
        AssignmentApplicationLookupValidatorImpl validator = new AssignmentApplicationLookupValidatorImpl(
                repositoryFinding(assignment), new AssignmentMustExistForTenantRuleImpl());

        StepVerifier.create(validator.execute(new AssignmentOwnershipQuery(ASSIGNMENT, TENANT)))
                .assertNext(result -> assertThat(result).isEqualTo(APPLICATION))
                .verifyComplete();
    }

    @Test
    void refuses_an_assignment_that_does_not_exist_for_the_tenant() {
        AssignmentApplicationLookupValidatorImpl validator = new AssignmentApplicationLookupValidatorImpl(
                repositoryFinding(null), new AssignmentMustExistForTenantRuleImpl());

        StepVerifier.create(validator.execute(new AssignmentOwnershipQuery(ASSIGNMENT, TENANT)))
                .expectError(AssignmentNotFoundException.class)
                .verify();
    }

    private static AssignmentRepository repositoryFinding(Assignment found) {
        return new AssignmentRepository() {
            @Override
            public Mono<Boolean> existsActiveByUserApplicationRole(UserId userId, ApplicationId applicationId,
                                                                   RoleId roleId, Instant now) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Mono<Assignment> findByIdForTenant(AssignmentId assignmentId, TenantId tenantId) {
                return found == null ? Mono.empty() : Mono.just(found);
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
                throw new UnsupportedOperationException();
            }
        };
    }
}
