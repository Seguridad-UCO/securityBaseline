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
import co.edu.uco.seguridad.shared.audit.AdministrationEvent;
import co.edu.uco.seguridad.shared.audit.AdministrationOperation;
import co.edu.uco.seguridad.shared.audit.AdministrationOutcome;
import co.edu.uco.seguridad.shared.audit.TestAdministrationAuditRepositories;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * HU-018: gatea AssignRoleUseCase — incondicional, la asignación siempre trae applicationId.
 */
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
    private static final UUID FIXED_UUID = UUID.randomUUID();
    private static final Instant FIXED_INSTANT = Instant.parse("2026-09-15T00:00:00Z");

    @Test
    void creates_the_assignment_when_the_principal_administers_the_application_and_audits_allowed() {
        List<AssignRoleRequest> received = new ArrayList<>();
        List<AdministrationEvent> audited = new ArrayList<>();
        AdministerAssignmentCreationUseCaseImpl useCase = new AdministerAssignmentCreationUseCaseImpl(
                allows(), assignRoleCapturing(received), TestAdministrationAuditRepositories.capturing(audited),
                () -> FIXED_UUID, () -> FIXED_INSTANT);

        StepVerifier.create(useCase.execute(new AdministerAssignmentCreationRequest(ADMINISTRATION, ASSIGNMENT_REQUEST)))
                .assertNext(response -> assertThat(response).isEqualTo(RESPONSE))
                .verifyComplete();
        assertThat(received).containsExactly(ASSIGNMENT_REQUEST);
        assertThat(audited).hasSize(1);
        assertThat(audited.getFirst().operation()).isEqualTo(AdministrationOperation.ROLE_ASSIGNED);
        assertThat(audited.getFirst().outcome()).isEqualTo(AdministrationOutcome.ALLOWED);
    }

    @Test
    void never_creates_the_assignment_when_the_principal_does_not_administer_the_application_and_audits_denied() {
        AssignRoleUseCase assignRole = input -> {
            throw new AssertionError("must not reach AssignRoleUseCase");
        };
        List<AdministrationEvent> audited = new ArrayList<>();
        AdministerAssignmentCreationUseCaseImpl useCase = new AdministerAssignmentCreationUseCaseImpl(denies(), assignRole,
                TestAdministrationAuditRepositories.capturing(audited), () -> FIXED_UUID, () -> FIXED_INSTANT);

        StepVerifier.create(useCase.execute(new AdministerAssignmentCreationRequest(ADMINISTRATION, ASSIGNMENT_REQUEST)))
                .expectError(NotAuthorizedToAdministerException.class)
                .verify();
        assertThat(audited).hasSize(1);
        assertThat(audited.getFirst().outcome()).isEqualTo(AdministrationOutcome.DENIED);
    }

    @Test
    void does_not_block_the_result_when_the_audit_repository_fails() {
        List<AssignRoleRequest> received = new ArrayList<>();
        AdministerAssignmentCreationUseCaseImpl useCase = new AdministerAssignmentCreationUseCaseImpl(
                allows(), assignRoleCapturing(received), TestAdministrationAuditRepositories.failing(), () -> FIXED_UUID,
                () -> FIXED_INSTANT);

        StepVerifier.create(useCase.execute(new AdministerAssignmentCreationRequest(ADMINISTRATION, ASSIGNMENT_REQUEST)))
                .assertNext(response -> assertThat(response).isEqualTo(RESPONSE))
                .verifyComplete();
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
