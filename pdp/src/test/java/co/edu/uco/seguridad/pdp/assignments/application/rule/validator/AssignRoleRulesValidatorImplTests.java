package co.edu.uco.seguridad.pdp.assignments.application.rule.validator;

import co.edu.uco.seguridad.pdp.applications.application.rule.validator.ApplicationMustExistForTenantValidator;
import co.edu.uco.seguridad.pdp.applications.domain.exception.ApplicationNotFoundException;
import co.edu.uco.seguridad.pdp.assignments.application.primaryport.request.AssignRoleRequest;
import co.edu.uco.seguridad.pdp.assignments.application.rule.validator.impl.AssignRoleRulesValidatorImpl;
import co.edu.uco.seguridad.pdp.assignments.application.secondaryport.repository.AssignmentRepository;
import co.edu.uco.seguridad.pdp.assignments.domain.Assignment;
import co.edu.uco.seguridad.pdp.assignments.domain.AssignmentCriteria;
import co.edu.uco.seguridad.pdp.assignments.domain.exception.DuplicateAssignmentException;
import co.edu.uco.seguridad.pdp.assignments.domain.model.AssignmentId;
import co.edu.uco.seguridad.pdp.commons.model.*;
import co.edu.uco.seguridad.pdp.identity.domain.exception.UserNotFoundException;
import co.edu.uco.seguridad.pdp.roles.application.rule.validator.RoleScopeMustCoverApplicationValidator;
import co.edu.uco.seguridad.pdp.roles.domain.exception.ApplicationOutsideRoleScopeException;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * R1 → R2 → R3 → R4 en ese orden. Cada rechazo se aísla con un colaborador que lanza y los
 * anteriores en "poison pill", para probar que la validación se detiene ahí y no sigue consultando.
 */
class AssignRoleRulesValidatorImplTests {

    private static final TenantId TENANT = new TenantId("universidad-uco");
    private static final UserId USER = new UserId(UUID.randomUUID());
    private static final ApplicationId APPLICATION = new ApplicationId(UUID.randomUUID());
    private static final RoleId ROLE = new RoleId(UUID.randomUUID());
    private static final Instant NOW = Instant.parse("2026-09-12T00:00:00Z");

    private static final ApplicationMustExistForTenantValidator NEVER_CALLED_APPLICATION_VALIDATOR = query -> {
        throw new AssertionError("must not check the application when the user does not exist");
    };
    private static final RoleScopeMustCoverApplicationValidator NEVER_CALLED_ROLE_VALIDATOR = query -> {
        throw new AssertionError("must not check the role when an earlier rule already rejected");
    };

    @Test
    void rejects_when_the_user_does_not_exist() {
        AssignRoleRulesValidatorImpl validator = new AssignRoleRulesValidatorImpl(
                userId -> Mono.error(new UserNotFoundException(userId)), NEVER_CALLED_APPLICATION_VALIDATOR,
                NEVER_CALLED_ROLE_VALIDATOR, coverage -> {
        }, unreachableRepository(), () -> NOW);

        StepVerifier.create(validator.execute(request()))
                .expectError(UserNotFoundException.class)
                .verify();
    }

    @Test
    void rejects_when_the_application_does_not_exist() {
        AssignRoleRulesValidatorImpl validator = new AssignRoleRulesValidatorImpl(
                userId -> Mono.empty(), query -> Mono.error(new ApplicationNotFoundException(query.applicationId())),
                NEVER_CALLED_ROLE_VALIDATOR, coverage -> {
        }, unreachableRepository(), () -> NOW);

        StepVerifier.create(validator.execute(request()))
                .expectError(ApplicationNotFoundException.class)
                .verify();
    }

    @Test
    void rejects_when_the_role_does_not_cover_the_application() {
        AssignRoleRulesValidatorImpl validator = new AssignRoleRulesValidatorImpl(
                userId -> Mono.empty(), query -> Mono.empty(),
                query -> Mono.error(new ApplicationOutsideRoleScopeException(APPLICATION)), coverage -> {
        },
                unreachableRepository(), () -> NOW);

        StepVerifier.create(validator.execute(request()))
                .expectError(ApplicationOutsideRoleScopeException.class)
                .verify();
    }

    @Test
    void rejects_when_the_assignment_is_already_active() {
        AssignRoleRulesValidatorImpl validator = new AssignRoleRulesValidatorImpl(
                userId -> Mono.empty(), query -> Mono.empty(), query -> Mono.empty(),
                input -> {
                    throw new DuplicateAssignmentException(USER, APPLICATION, ROLE);
                },
                repositoryReporting(true), () -> NOW);

        StepVerifier.create(validator.execute(request()))
                .expectError(DuplicateAssignmentException.class)
                .verify();
    }

    @Test
    void completes_when_every_rule_passes() {
        AssignRoleRulesValidatorImpl validator = new AssignRoleRulesValidatorImpl(
                userId -> Mono.empty(), query -> Mono.empty(), query -> Mono.empty(), input -> {
        },
                repositoryReporting(false), () -> NOW);

        StepVerifier.create(validator.execute(request())).verifyComplete();
    }

    private static AssignRoleRequest request() {
        return new AssignRoleRequest(TENANT, USER, APPLICATION, ROLE);
    }

    private static AssignmentRepository repositoryReporting(boolean active) {
        return new AssignmentRepository() {
            @Override
            public Mono<Boolean> existsActiveByUserApplicationRole(UserId userId, ApplicationId applicationId, RoleId roleId,
                                                                   Instant now) {
                return Mono.just(active);
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
                throw new UnsupportedOperationException();
            }
        };
    }

    private static AssignmentRepository unreachableRepository() {
        return new AssignmentRepository() {
            @Override
            public Mono<Boolean> existsActiveByUserApplicationRole(UserId userId, ApplicationId applicationId, RoleId roleId,
                                                                   Instant now) {
                throw new AssertionError("must not check for a duplicate before the earlier rules pass");
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
                throw new UnsupportedOperationException();
            }
        };
    }
}
