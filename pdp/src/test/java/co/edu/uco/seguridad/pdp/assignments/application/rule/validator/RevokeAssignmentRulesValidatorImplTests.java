package co.edu.uco.seguridad.pdp.assignments.application.rule.validator;

import co.edu.uco.seguridad.pdp.assignments.application.primaryport.request.RevokeAssignmentRequest;
import co.edu.uco.seguridad.pdp.assignments.application.rule.validator.impl.RevokeAssignmentRulesValidatorImpl;
import co.edu.uco.seguridad.pdp.assignments.application.secondaryport.repository.AssignmentRepository;
import co.edu.uco.seguridad.pdp.assignments.domain.Assignment;
import co.edu.uco.seguridad.pdp.assignments.domain.AssignmentCriteria;
import co.edu.uco.seguridad.pdp.assignments.domain.exception.AssignmentNotFoundException;
import co.edu.uco.seguridad.pdp.assignments.domain.model.AssignmentId;
import co.edu.uco.seguridad.pdp.assignments.domain.rule.AssignmentMustExistForTenantRule;
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

class RevokeAssignmentRulesValidatorImplTests {

    private static final AssignmentId ID = new AssignmentId(UUID.randomUUID());
    private static final TenantId TENANT = new TenantId("universidad-uco");
    private static final Assignment ASSIGNMENT = Assignment.assign(ID, new UserId(UUID.randomUUID()), TENANT,
            new ApplicationId(UUID.randomUUID()), new RoleId(UUID.randomUUID()), Instant.parse("2026-09-12T00:00:00Z"));

    @Test
    void rejects_when_the_assignment_is_not_found_for_the_tenant() {
        RevokeAssignmentRulesValidatorImpl validator = new RevokeAssignmentRulesValidatorImpl(
                repositoryReturning(Mono.empty()), rejecting());

        StepVerifier.create(validator.execute(request()))
                .expectError(AssignmentNotFoundException.class)
                .verify();
    }

    @Test
    void returns_the_assignment_when_found() {
        RevokeAssignmentRulesValidatorImpl validator = new RevokeAssignmentRulesValidatorImpl(
                repositoryReturning(Mono.just(ASSIGNMENT)), accepting());

        StepVerifier.create(validator.execute(request())).expectNext(ASSIGNMENT).verifyComplete();
    }

    private static RevokeAssignmentRequest request() {
        return new RevokeAssignmentRequest(ID, TENANT);
    }

    private static AssignmentMustExistForTenantRule rejecting() {
        return input -> { throw new AssignmentNotFoundException(input.assignmentId()); };
    }

    private static AssignmentMustExistForTenantRule accepting() {
        return input -> { };
    }

    private static AssignmentRepository repositoryReturning(Mono<Assignment> result) {
        return new AssignmentRepository() {
            @Override
            public Mono<Boolean> existsActiveByUserApplicationRole(UserId userId, ApplicationId applicationId, RoleId roleId,
                    Instant now) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Mono<Assignment> findByIdForTenant(AssignmentId assignmentId, TenantId tenantId) {
                return result;
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
