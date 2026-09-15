package co.edu.uco.seguridad.pdp.authorization.application.usecase.impl;

import co.edu.uco.seguridad.pdp.assignments.application.primaryport.request.RevokeAssignmentRequest;
import co.edu.uco.seguridad.pdp.assignments.application.usecase.RevokeAssignmentUseCase;
import co.edu.uco.seguridad.pdp.assignments.domain.model.AssignmentId;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AdministerAssignmentRevocationRequest;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AdministrationRequest;
import co.edu.uco.seguridad.pdp.authorization.application.rule.validator.PrincipalMustBeApplicationAdministratorValidator;
import co.edu.uco.seguridad.pdp.authorization.domain.exception.NotAuthorizedToAdministerException;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.commons.model.UserId;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** HU-018: gatea RevokeAssignmentUseCase — la administración ya viene resuelta por el interactor. */
class AdministerAssignmentRevocationUseCaseImplTests {

    private static final TenantId TENANT = new TenantId("universidad-uco");
    private static final ApplicationId APPLICATION = new ApplicationId(UUID.randomUUID());
    private static final UserId ADMIN_USER = new UserId(UUID.randomUUID());
    private static final AdministrationRequest ADMINISTRATION =
            new AdministrationRequest(TENANT, APPLICATION, ADMIN_USER, "test-subject", Set.of());
    private static final RevokeAssignmentRequest REVOCATION_REQUEST =
            new RevokeAssignmentRequest(new AssignmentId(UUID.randomUUID()), TENANT);

    @Test
    void revokes_the_assignment_when_the_principal_administers_the_application() {
        List<RevokeAssignmentRequest> received = new ArrayList<>();
        AdministerAssignmentRevocationUseCaseImpl useCase = new AdministerAssignmentRevocationUseCaseImpl(
                allows(), revokeAssignmentCapturing(received));

        StepVerifier.create(useCase.execute(new AdministerAssignmentRevocationRequest(ADMINISTRATION, REVOCATION_REQUEST)))
                .verifyComplete();
        assertThat(received).containsExactly(REVOCATION_REQUEST);
    }

    @Test
    void never_revokes_the_assignment_when_the_principal_does_not_administer_the_application() {
        RevokeAssignmentUseCase revokeAssignment = input -> {
            throw new AssertionError("must not reach RevokeAssignmentUseCase");
        };
        AdministerAssignmentRevocationUseCaseImpl useCase = new AdministerAssignmentRevocationUseCaseImpl(
                denies(), revokeAssignment);

        StepVerifier.create(useCase.execute(new AdministerAssignmentRevocationRequest(ADMINISTRATION, REVOCATION_REQUEST)))
                .expectError(NotAuthorizedToAdministerException.class)
                .verify();
    }

    private static PrincipalMustBeApplicationAdministratorValidator allows() {
        return request -> Mono.empty();
    }

    private static PrincipalMustBeApplicationAdministratorValidator denies() {
        return request -> Mono.error(new NotAuthorizedToAdministerException(TENANT, APPLICATION));
    }

    private static RevokeAssignmentUseCase revokeAssignmentCapturing(List<RevokeAssignmentRequest> received) {
        return input -> {
            received.add(input);
            return Mono.empty();
        };
    }
}
