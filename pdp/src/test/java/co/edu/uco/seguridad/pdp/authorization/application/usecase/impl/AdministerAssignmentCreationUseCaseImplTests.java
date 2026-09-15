package co.edu.uco.seguridad.pdp.authorization.application.usecase.impl;

import co.edu.uco.seguridad.pdp.assignments.application.primaryport.request.AssignRoleRequest;
import co.edu.uco.seguridad.pdp.assignments.application.primaryport.response.AssignmentResponse;
import co.edu.uco.seguridad.pdp.assignments.application.usecase.AssignRoleUseCase;
import co.edu.uco.seguridad.pdp.assignments.domain.model.AssignmentId;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AdministerAssignmentCreationRequest;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AdministrationRequest;
import co.edu.uco.seguridad.pdp.authorization.application.rule.validator.PrincipalMustBeApplicationAdministratorValidator;
import co.edu.uco.seguridad.pdp.authorization.domain.exception.NotAuthorizedToAdministerException;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.RoleId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.commons.model.UserId;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** HU-018: gatea AssignRoleUseCase — incondicional, la asignación siempre trae applicationId. */
class AdministerAssignmentCreationUseCaseImplTests {

    private static final TenantId TENANT = new TenantId("universidad-uco");
    private static final ApplicationId APPLICATION = new ApplicationId(UUID.randomUUID());
    private static final UserId ADMIN_USER = new UserId(UUID.randomUUID());
    private static final AdministrationRequest ADMINISTRATION =
            new AdministrationRequest(TENANT, APPLICATION, ADMIN_USER, "test-subject", Set.of());
    private static final AssignRoleRequest ASSIGNMENT_REQUEST =
            new AssignRoleRequest(TENANT, new UserId(UUID.randomUUID()), APPLICATION, new RoleId(UUID.randomUUID()));
    private static final AssignmentResponse RESPONSE = new AssignmentResponse(new AssignmentId(UUID.randomUUID()),
            ASSIGNMENT_REQUEST.userId(), TENANT, APPLICATION, ASSIGNMENT_REQUEST.roleId(),
            Instant.parse("2026-09-15T00:00:00Z"), Optional.empty());

    @Test
    void creates_the_assignment_when_the_principal_administers_the_application() {
        List<AssignRoleRequest> received = new ArrayList<>();
        AdministerAssignmentCreationUseCaseImpl useCase = new AdministerAssignmentCreationUseCaseImpl(
                allows(), assignRoleCapturing(received));

        StepVerifier.create(useCase.execute(new AdministerAssignmentCreationRequest(ADMINISTRATION, ASSIGNMENT_REQUEST)))
                .assertNext(response -> assertThat(response).isEqualTo(RESPONSE))
                .verifyComplete();
        assertThat(received).containsExactly(ASSIGNMENT_REQUEST);
    }

    @Test
    void never_creates_the_assignment_when_the_principal_does_not_administer_the_application() {
        AssignRoleUseCase assignRole = input -> {
            throw new AssertionError("must not reach AssignRoleUseCase");
        };
        AdministerAssignmentCreationUseCaseImpl useCase = new AdministerAssignmentCreationUseCaseImpl(denies(), assignRole);

        StepVerifier.create(useCase.execute(new AdministerAssignmentCreationRequest(ADMINISTRATION, ASSIGNMENT_REQUEST)))
                .expectError(NotAuthorizedToAdministerException.class)
                .verify();
    }

    private static PrincipalMustBeApplicationAdministratorValidator allows() {
        return request -> Mono.empty();
    }

    private static PrincipalMustBeApplicationAdministratorValidator denies() {
        return request -> Mono.error(new NotAuthorizedToAdministerException(TENANT, APPLICATION));
    }

    private static AssignRoleUseCase assignRoleCapturing(List<AssignRoleRequest> received) {
        return input -> {
            received.add(input);
            return Mono.just(RESPONSE);
        };
    }
}
