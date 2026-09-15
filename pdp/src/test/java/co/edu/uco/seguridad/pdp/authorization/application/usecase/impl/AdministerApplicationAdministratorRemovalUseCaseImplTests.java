package co.edu.uco.seguridad.pdp.authorization.application.usecase.impl;

import co.edu.uco.seguridad.pdp.assignments.application.primaryport.request.RemoveApplicationAdministratorRequest;
import co.edu.uco.seguridad.pdp.assignments.application.usecase.RemoveApplicationAdministratorUseCase;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AdministerApplicationAdministratorRemovalRequest;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AdministrationRequest;
import co.edu.uco.seguridad.pdp.authorization.application.rule.validator.PrincipalMustBeApplicationAdministratorValidator;
import co.edu.uco.seguridad.pdp.authorization.domain.exception.NotAuthorizedToAdministerException;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
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
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** HU-020: gatea RemoveApplicationAdministratorUseCase tras HU-009. */
class AdministerApplicationAdministratorRemovalUseCaseImplTests {

    private static final TenantId TENANT = new TenantId("universidad-uco");
    private static final ApplicationId APPLICATION = new ApplicationId(UUID.randomUUID());
    private static final UserId ADMIN_USER = new UserId(UUID.randomUUID());
    private static final AdministrationRequest ADMINISTRATION =
            new AdministrationRequest(TENANT, APPLICATION, ADMIN_USER, "test-subject", Set.of());
    private static final RemoveApplicationAdministratorRequest REMOVAL_REQUEST =
            new RemoveApplicationAdministratorRequest(TENANT, APPLICATION, new UserId(UUID.randomUUID()));
    private static final UUID FIXED_UUID = UUID.randomUUID();
    private static final Instant FIXED_INSTANT = Instant.parse("2026-09-15T00:00:00Z");

    @Test
    void removes_the_administrator_when_the_principal_administers_the_application_and_audits_allowed() {
        List<RemoveApplicationAdministratorRequest> received = new ArrayList<>();
        List<AdministrationEvent> audited = new ArrayList<>();
        AdministerApplicationAdministratorRemovalUseCaseImpl useCase =
                new AdministerApplicationAdministratorRemovalUseCaseImpl(allows(), removeCapturing(received),
                        TestAdministrationAuditRepositories.capturing(audited), () -> FIXED_UUID, () -> FIXED_INSTANT);

        StepVerifier.create(useCase.execute(
                        new AdministerApplicationAdministratorRemovalRequest(ADMINISTRATION, REMOVAL_REQUEST)))
                .verifyComplete();
        assertThat(received).containsExactly(REMOVAL_REQUEST);
        assertThat(audited).hasSize(1);
        assertThat(audited.getFirst().operation()).isEqualTo(AdministrationOperation.ADMINISTRATOR_REMOVED);
        assertThat(audited.getFirst().outcome()).isEqualTo(AdministrationOutcome.ALLOWED);
    }

    @Test
    void never_removes_the_administrator_when_the_principal_does_not_administer_the_application_and_audits_denied() {
        RemoveApplicationAdministratorUseCase removeApplicationAdministrator = input -> {
            throw new AssertionError("must not reach RemoveApplicationAdministratorUseCase");
        };
        List<AdministrationEvent> audited = new ArrayList<>();
        AdministerApplicationAdministratorRemovalUseCaseImpl useCase =
                new AdministerApplicationAdministratorRemovalUseCaseImpl(denies(), removeApplicationAdministrator,
                        TestAdministrationAuditRepositories.capturing(audited), () -> FIXED_UUID, () -> FIXED_INSTANT);

        StepVerifier.create(useCase.execute(
                        new AdministerApplicationAdministratorRemovalRequest(ADMINISTRATION, REMOVAL_REQUEST)))
                .expectError(NotAuthorizedToAdministerException.class)
                .verify();
        assertThat(audited).hasSize(1);
        assertThat(audited.getFirst().outcome()).isEqualTo(AdministrationOutcome.DENIED);
    }

    @Test
    void does_not_block_the_result_when_the_audit_repository_fails() {
        List<RemoveApplicationAdministratorRequest> received = new ArrayList<>();
        AdministerApplicationAdministratorRemovalUseCaseImpl useCase =
                new AdministerApplicationAdministratorRemovalUseCaseImpl(allows(), removeCapturing(received),
                        TestAdministrationAuditRepositories.failing(), () -> FIXED_UUID, () -> FIXED_INSTANT);

        StepVerifier.create(useCase.execute(
                        new AdministerApplicationAdministratorRemovalRequest(ADMINISTRATION, REMOVAL_REQUEST)))
                .verifyComplete();
    }

    private static PrincipalMustBeApplicationAdministratorValidator allows() {
        return request -> Mono.empty();
    }

    private static PrincipalMustBeApplicationAdministratorValidator denies() {
        return request -> Mono.error(new NotAuthorizedToAdministerException(TENANT, APPLICATION));
    }

    private static RemoveApplicationAdministratorUseCase removeCapturing(
            List<RemoveApplicationAdministratorRequest> received) {
        return input -> {
            received.add(input);
            return Mono.empty();
        };
    }
}
