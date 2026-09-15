package co.edu.uco.seguridad.pdp.authorization.application.usecase.impl;

import co.edu.uco.seguridad.pdp.assignments.application.primaryport.request.AssignApplicationAdministratorRequest;
import co.edu.uco.seguridad.pdp.assignments.application.primaryport.response.AssignmentResponse;
import co.edu.uco.seguridad.pdp.assignments.application.usecase.AssignApplicationAdministratorUseCase;
import co.edu.uco.seguridad.pdp.assignments.domain.model.AssignmentId;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AdministerApplicationAdministratorAssignmentRequest;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** HU-020: gatea AssignApplicationAdministratorUseCase (HU-015) tras HU-009. */
class AdministerApplicationAdministratorAssignmentUseCaseImplTests {

    private static final TenantId TENANT = new TenantId("universidad-uco");
    private static final ApplicationId APPLICATION = new ApplicationId(UUID.randomUUID());
    private static final UserId ADMIN_USER = new UserId(UUID.randomUUID());
    private static final AdministrationRequest ADMINISTRATION =
            new AdministrationRequest(TENANT, APPLICATION, ADMIN_USER, "test-subject", Set.of());
    private static final AssignApplicationAdministratorRequest ASSIGNMENT_REQUEST =
            new AssignApplicationAdministratorRequest(TENANT, APPLICATION, new UserId(UUID.randomUUID()));
    private static final AssignmentResponse RESPONSE = new AssignmentResponse(new AssignmentId(UUID.randomUUID()),
            ASSIGNMENT_REQUEST.userId(), TENANT, APPLICATION, new RoleId(UUID.randomUUID()),
            Instant.parse("2026-09-15T00:00:00Z"), Optional.empty());
    private static final UUID FIXED_UUID = UUID.randomUUID();
    private static final Instant FIXED_INSTANT = Instant.parse("2026-09-15T00:00:00Z");

    @Test
    void assigns_the_administrator_when_the_principal_administers_the_application_and_audits_allowed() {
        List<AssignApplicationAdministratorRequest> received = new ArrayList<>();
        List<AdministrationEvent> audited = new ArrayList<>();
        AdministerApplicationAdministratorAssignmentUseCaseImpl useCase =
                new AdministerApplicationAdministratorAssignmentUseCaseImpl(allows(), assignCapturing(received),
                        TestAdministrationAuditRepositories.capturing(audited), () -> FIXED_UUID, () -> FIXED_INSTANT);

        StepVerifier.create(useCase.execute(
                        new AdministerApplicationAdministratorAssignmentRequest(ADMINISTRATION, ASSIGNMENT_REQUEST)))
                .expectNext(RESPONSE)
                .verifyComplete();
        assertThat(received).containsExactly(ASSIGNMENT_REQUEST);
        assertThat(audited).hasSize(1);
        assertThat(audited.getFirst().operation()).isEqualTo(AdministrationOperation.ADMINISTRATOR_ASSIGNED);
        assertThat(audited.getFirst().outcome()).isEqualTo(AdministrationOutcome.ALLOWED);
    }

    @Test
    void never_assigns_the_administrator_when_the_principal_does_not_administer_the_application_and_audits_denied() {
        AssignApplicationAdministratorUseCase assignApplicationAdministrator = input -> {
            throw new AssertionError("must not reach AssignApplicationAdministratorUseCase");
        };
        List<AdministrationEvent> audited = new ArrayList<>();
        AdministerApplicationAdministratorAssignmentUseCaseImpl useCase =
                new AdministerApplicationAdministratorAssignmentUseCaseImpl(denies(), assignApplicationAdministrator,
                        TestAdministrationAuditRepositories.capturing(audited), () -> FIXED_UUID, () -> FIXED_INSTANT);

        StepVerifier.create(useCase.execute(
                        new AdministerApplicationAdministratorAssignmentRequest(ADMINISTRATION, ASSIGNMENT_REQUEST)))
                .expectError(NotAuthorizedToAdministerException.class)
                .verify();
        assertThat(audited).hasSize(1);
        assertThat(audited.getFirst().outcome()).isEqualTo(AdministrationOutcome.DENIED);
    }

    @Test
    void does_not_block_the_result_when_the_audit_repository_fails() {
        List<AssignApplicationAdministratorRequest> received = new ArrayList<>();
        AdministerApplicationAdministratorAssignmentUseCaseImpl useCase =
                new AdministerApplicationAdministratorAssignmentUseCaseImpl(allows(), assignCapturing(received),
                        TestAdministrationAuditRepositories.failing(), () -> FIXED_UUID, () -> FIXED_INSTANT);

        StepVerifier.create(useCase.execute(
                        new AdministerApplicationAdministratorAssignmentRequest(ADMINISTRATION, ASSIGNMENT_REQUEST)))
                .expectNext(RESPONSE)
                .verifyComplete();
    }

    private static PrincipalMustBeApplicationAdministratorValidator allows() {
        return request -> Mono.empty();
    }

    private static PrincipalMustBeApplicationAdministratorValidator denies() {
        return request -> Mono.error(new NotAuthorizedToAdministerException(TENANT, APPLICATION));
    }

    private static AssignApplicationAdministratorUseCase assignCapturing(
            List<AssignApplicationAdministratorRequest> received) {
        return input -> {
            received.add(input);
            return Mono.just(RESPONSE);
        };
    }
}
