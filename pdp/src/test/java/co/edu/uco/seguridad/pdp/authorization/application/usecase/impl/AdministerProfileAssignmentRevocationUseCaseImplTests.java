package co.edu.uco.seguridad.pdp.authorization.application.usecase.impl;

import co.edu.uco.seguridad.pdp.assignments.application.primaryport.request.RevokeProfileAssignmentRequest;
import co.edu.uco.seguridad.pdp.assignments.application.usecase.RevokeProfileAssignmentUseCase;
import co.edu.uco.seguridad.pdp.assignments.domain.model.ProfileAssignmentId;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AdministerProfileAssignmentRevocationRequest;
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

/** HU-019: gatea RevokeProfileAssignmentUseCase — la administración ya viene resuelta por el interactor. */
class AdministerProfileAssignmentRevocationUseCaseImplTests {

    private static final TenantId TENANT = new TenantId("universidad-uco");
    private static final ApplicationId APPLICATION = new ApplicationId(UUID.randomUUID());
    private static final UserId ADMIN_USER = new UserId(UUID.randomUUID());
    private static final AdministrationRequest ADMINISTRATION =
            new AdministrationRequest(TENANT, APPLICATION, ADMIN_USER, "test-subject", Set.of());
    private static final RevokeProfileAssignmentRequest REVOCATION_REQUEST =
            new RevokeProfileAssignmentRequest(new ProfileAssignmentId(UUID.randomUUID()), TENANT);

    @Test
    void revokes_the_profile_assignment_when_the_principal_administers_the_application() {
        List<RevokeProfileAssignmentRequest> received = new ArrayList<>();
        AdministerProfileAssignmentRevocationUseCaseImpl useCase = new AdministerProfileAssignmentRevocationUseCaseImpl(
                allows(), revokeProfileAssignmentCapturing(received));

        StepVerifier.create(useCase.execute(new AdministerProfileAssignmentRevocationRequest(ADMINISTRATION, REVOCATION_REQUEST)))
                .verifyComplete();
        assertThat(received).containsExactly(REVOCATION_REQUEST);
    }

    @Test
    void never_revokes_the_profile_assignment_when_the_principal_does_not_administer_the_application() {
        RevokeProfileAssignmentUseCase revokeProfileAssignment = input -> {
            throw new AssertionError("must not reach RevokeProfileAssignmentUseCase");
        };
        AdministerProfileAssignmentRevocationUseCaseImpl useCase = new AdministerProfileAssignmentRevocationUseCaseImpl(
                denies(), revokeProfileAssignment);

        StepVerifier.create(useCase.execute(new AdministerProfileAssignmentRevocationRequest(ADMINISTRATION, REVOCATION_REQUEST)))
                .expectError(NotAuthorizedToAdministerException.class)
                .verify();
    }

    private static PrincipalMustBeApplicationAdministratorValidator allows() {
        return request -> Mono.empty();
    }

    private static PrincipalMustBeApplicationAdministratorValidator denies() {
        return request -> Mono.error(new NotAuthorizedToAdministerException(TENANT, APPLICATION));
    }

    private static RevokeProfileAssignmentUseCase revokeProfileAssignmentCapturing(
            List<RevokeProfileAssignmentRequest> received) {
        return input -> {
            received.add(input);
            return Mono.empty();
        };
    }
}
