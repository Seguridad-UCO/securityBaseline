package co.edu.uco.seguridad.pdp.authorization.application.usecase.impl;

import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AdministerRoleDefinitionRequest;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AdministrationRequest;
import co.edu.uco.seguridad.pdp.authorization.application.rule.validator.PrincipalMustBeApplicationAdministratorValidator;
import co.edu.uco.seguridad.pdp.authorization.domain.exception.NotAuthorizedToAdministerException;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.commons.model.UserId;
import co.edu.uco.seguridad.pdp.roles.application.primaryport.request.DefineRoleRequest;
import co.edu.uco.seguridad.pdp.roles.application.primaryport.response.RoleResponse;
import co.edu.uco.seguridad.pdp.roles.application.usecase.DefineRoleUseCase;
import co.edu.uco.seguridad.pdp.roles.domain.model.RoleName;
import co.edu.uco.seguridad.pdp.roles.domain.model.RoleScope;
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

/**
 * HU-016: gatea DefineRoleUseCase solo cuando hay administración que exigir (rol APPLICATION) —
 * un rol TENANT (administration vacío) delega directo, sin gate.
 */
class AdministerRoleDefinitionUseCaseImplTests {

    private static final TenantId TENANT = new TenantId("universidad-uco");
    private static final ApplicationId APPLICATION = new ApplicationId(UUID.randomUUID());
    private static final UserId USER = new UserId(UUID.randomUUID());
    private static final AdministrationRequest ADMINISTRATION =
            new AdministrationRequest(TENANT, APPLICATION, USER, "test-subject", Set.of());
    private static final DefineRoleRequest APPLICATION_SCOPED_ROLE =
            new DefineRoleRequest(new RoleName("Docente"), RoleScope.ofApplication(TENANT, APPLICATION));
    private static final DefineRoleRequest TENANT_SCOPED_ROLE =
            new DefineRoleRequest(new RoleName("Coordinador"), RoleScope.ofTenant(TENANT));
    private static final RoleResponse RESPONSE = new RoleResponse(new co.edu.uco.seguridad.pdp.commons.model.RoleId(
            UUID.randomUUID()), new RoleName("Docente"), RoleScope.ofApplication(TENANT, APPLICATION), Set.of(),
            Instant.parse("2026-09-14T00:00:00Z"));
    private static final UUID FIXED_UUID = UUID.randomUUID();
    private static final Instant FIXED_INSTANT = Instant.parse("2026-09-15T00:00:00Z");

    @Test
    void defines_the_role_when_the_principal_administers_the_application_and_audits_allowed() {
        List<DefineRoleRequest> received = new ArrayList<>();
        List<AdministrationEvent> audited = new ArrayList<>();
        AdministerRoleDefinitionUseCaseImpl useCase = new AdministerRoleDefinitionUseCaseImpl(
                allows(), defineRoleCapturing(received), TestAdministrationAuditRepositories.capturing(audited),
                () -> FIXED_UUID, () -> FIXED_INSTANT);

        StepVerifier.create(useCase.execute(new AdministerRoleDefinitionRequest(Optional.of(ADMINISTRATION), APPLICATION_SCOPED_ROLE)))
                .assertNext(response -> assertThat(response).isEqualTo(RESPONSE))
                .verifyComplete();
        assertThat(received).containsExactly(APPLICATION_SCOPED_ROLE);
        assertThat(audited).hasSize(1);
        assertThat(audited.getFirst().operation()).isEqualTo(AdministrationOperation.ROLE_DEFINED);
        assertThat(audited.getFirst().outcome()).isEqualTo(AdministrationOutcome.ALLOWED);
    }

    @Test
    void never_defines_the_role_when_the_principal_does_not_administer_the_application_and_audits_denied() {
        DefineRoleUseCase defineRole = input -> {
            throw new AssertionError("must not reach DefineRoleUseCase");
        };
        List<AdministrationEvent> audited = new ArrayList<>();
        AdministerRoleDefinitionUseCaseImpl useCase = new AdministerRoleDefinitionUseCaseImpl(denies(), defineRole,
                TestAdministrationAuditRepositories.capturing(audited), () -> FIXED_UUID, () -> FIXED_INSTANT);

        StepVerifier.create(useCase.execute(new AdministerRoleDefinitionRequest(Optional.of(ADMINISTRATION), APPLICATION_SCOPED_ROLE)))
                .expectError(NotAuthorizedToAdministerException.class)
                .verify();
        assertThat(audited).hasSize(1);
        assertThat(audited.getFirst().outcome()).isEqualTo(AdministrationOutcome.DENIED);
    }

    @Test
    void defines_a_tenant_scoped_role_without_gating_or_auditing_when_administration_is_empty() {
        List<DefineRoleRequest> received = new ArrayList<>();
        PrincipalMustBeApplicationAdministratorValidator neverCalled = request -> {
            throw new AssertionError("must not reach the administration gate");
        };
        AdministerRoleDefinitionUseCaseImpl useCase = new AdministerRoleDefinitionUseCaseImpl(
                neverCalled, defineRoleCapturing(received), TestAdministrationAuditRepositories.unreachable(),
                () -> FIXED_UUID, () -> FIXED_INSTANT);

        StepVerifier.create(useCase.execute(new AdministerRoleDefinitionRequest(Optional.empty(), TENANT_SCOPED_ROLE)))
                .assertNext(response -> assertThat(response).isEqualTo(RESPONSE))
                .verifyComplete();
        assertThat(received).containsExactly(TENANT_SCOPED_ROLE);
    }

    @Test
    void does_not_block_the_result_when_the_audit_repository_fails() {
        List<DefineRoleRequest> received = new ArrayList<>();
        AdministerRoleDefinitionUseCaseImpl useCase = new AdministerRoleDefinitionUseCaseImpl(
                allows(), defineRoleCapturing(received), TestAdministrationAuditRepositories.failing(), () -> FIXED_UUID,
                () -> FIXED_INSTANT);

        StepVerifier.create(useCase.execute(new AdministerRoleDefinitionRequest(Optional.of(ADMINISTRATION), APPLICATION_SCOPED_ROLE)))
                .assertNext(response -> assertThat(response).isEqualTo(RESPONSE))
                .verifyComplete();
    }

    private static PrincipalMustBeApplicationAdministratorValidator allows() {
        return request -> Mono.empty();
    }

    private static PrincipalMustBeApplicationAdministratorValidator denies() {
        return request -> Mono.error(new NotAuthorizedToAdministerException(TENANT, APPLICATION));
    }

    private static DefineRoleUseCase defineRoleCapturing(List<DefineRoleRequest> received) {
        return input -> {
            received.add(input);
            return Mono.just(RESPONSE);
        };
    }
}
