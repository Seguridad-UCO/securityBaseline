package co.edu.uco.seguridad.pdp.authorization.application.usecase.impl;

import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AdministerRoleDefinitionRequest;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AdministrationRequest;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.CatalogAdministration;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.TenantAdministrationRequest;
import co.edu.uco.seguridad.pdp.authorization.application.rule.validator.PrincipalMustAdministerAtLeastOneApplicationValidator;
import co.edu.uco.seguridad.pdp.authorization.application.rule.validator.PrincipalMustBeApplicationAdministratorValidator;
import co.edu.uco.seguridad.pdp.authorization.domain.exception.MfaEvidenceRequiredException;
import co.edu.uco.seguridad.pdp.authorization.domain.exception.NotAuthorizedToAdministerException;
import co.edu.uco.seguridad.pdp.authorization.domain.exception.NotAuthorizedToAdministerTenantCatalogException;
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
import co.edu.uco.seguridad.shared.security.mfa.AuthenticationContextEvidence;
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
 * HU-016 + HU-036: gatea DefineRoleUseCase según el alcance del rol. Un rol {@code APPLICATION} exige
 * administrar esa aplicación (ADR-023); un rol {@code TENANT} exige administrar al menos una aplicación del
 * inquilino (ADR-036) — ya no existe el camino «sin gate». La auditoría del camino {@code TENANT} atribuye el
 * evento a la aplicación que concedió el permiso ({@code ALLOWED}) o a la primera evaluada ({@code DENIED}).
 */
class AdministerRoleDefinitionUseCaseImplTests {

    private static final TenantId TENANT = new TenantId("universidad-uco");
    private static final ApplicationId APPLICATION = new ApplicationId(UUID.randomUUID());
    private static final ApplicationId GRANTING_APPLICATION = new ApplicationId(UUID.fromString("00000000-0000-0000-0000-0000000000a2"));
    private static final ApplicationId FIRST_EVALUATED_APPLICATION = new ApplicationId(UUID.fromString("00000000-0000-0000-0000-0000000000a1"));
    private static final UserId USER = new UserId(UUID.randomUUID());
    private static final AdministrationRequest ADMINISTRATION =
            new AdministrationRequest(TENANT, APPLICATION, USER, "test-subject", Set.of());
    private static final TenantAdministrationRequest TENANT_ACTOR =
            new TenantAdministrationRequest(TENANT, USER, "test-subject", AuthenticationContextEvidence.NONE);
    private static final DefineRoleRequest APPLICATION_SCOPED_ROLE =
            new DefineRoleRequest(new RoleName("Docente"), RoleScope.ofApplication(TENANT, APPLICATION));
    private static final DefineRoleRequest TENANT_SCOPED_ROLE =
            new DefineRoleRequest(new RoleName("Coordinador"), RoleScope.ofTenant(TENANT));
    private static final RoleResponse RESPONSE = new RoleResponse(new co.edu.uco.seguridad.pdp.commons.model.RoleId(
            UUID.randomUUID()), new RoleName("Docente"), RoleScope.ofApplication(TENANT, APPLICATION), Set.of(),
            Instant.parse("2026-09-14T00:00:00Z"));
    private static final UUID FIXED_UUID = UUID.randomUUID();
    private static final Instant FIXED_INSTANT = Instant.parse("2026-09-15T00:00:00Z");

    private final List<AdministrationRequest> applicationGateCalls = new ArrayList<>();
    private final List<TenantAdministrationRequest> tenantGateCalls = new ArrayList<>();

    // ---- alcance APPLICATION (sin regresión, C13) ----------------------------------------------------------------

    @Test
    void defines_the_role_when_the_principal_administers_the_application_and_audits_allowed() {
        List<DefineRoleRequest> received = new ArrayList<>();
        List<AdministrationEvent> audited = new ArrayList<>();
        AdministerRoleDefinitionUseCaseImpl useCase = new AdministerRoleDefinitionUseCaseImpl(
                allows(), neverAskedAboutTheTenant(), defineRoleCapturing(received),
                TestAdministrationAuditRepositories.capturing(audited), () -> FIXED_UUID, () -> FIXED_INSTANT);

        StepVerifier.create(useCase.execute(new AdministerRoleDefinitionRequest(
                        new CatalogAdministration.OfApplication(ADMINISTRATION), APPLICATION_SCOPED_ROLE)))
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
        AdministerRoleDefinitionUseCaseImpl useCase = new AdministerRoleDefinitionUseCaseImpl(denies(),
                neverAskedAboutTheTenant(), defineRole, TestAdministrationAuditRepositories.capturing(audited),
                () -> FIXED_UUID, () -> FIXED_INSTANT);

        StepVerifier.create(useCase.execute(new AdministerRoleDefinitionRequest(
                        new CatalogAdministration.OfApplication(ADMINISTRATION), APPLICATION_SCOPED_ROLE)))
                .expectError(NotAuthorizedToAdministerException.class)
                .verify();
        assertThat(audited).hasSize(1);
        assertThat(audited.getFirst().outcome()).isEqualTo(AdministrationOutcome.DENIED);
    }

    @Test
    void does_not_block_the_result_when_the_audit_repository_fails() {
        List<DefineRoleRequest> received = new ArrayList<>();
        AdministerRoleDefinitionUseCaseImpl useCase = new AdministerRoleDefinitionUseCaseImpl(
                allows(), neverAskedAboutTheTenant(), defineRoleCapturing(received),
                TestAdministrationAuditRepositories.failing(), () -> FIXED_UUID, () -> FIXED_INSTANT);

        StepVerifier.create(useCase.execute(new AdministerRoleDefinitionRequest(
                        new CatalogAdministration.OfApplication(ADMINISTRATION), APPLICATION_SCOPED_ROLE)))
                .assertNext(response -> assertThat(response).isEqualTo(RESPONSE))
                .verifyComplete();
    }

    @Test
    void the_tenant_gate_is_not_invoked_for_an_application_role() {
        List<AdministrationEvent> audited = new ArrayList<>();
        AdministerRoleDefinitionUseCaseImpl useCase = new AdministerRoleDefinitionUseCaseImpl(
                allows(), tenantAllows(), defineRoleCapturing(new ArrayList<>()),
                TestAdministrationAuditRepositories.capturing(audited), () -> FIXED_UUID, () -> FIXED_INSTANT);

        StepVerifier.create(useCase.execute(new AdministerRoleDefinitionRequest(
                        new CatalogAdministration.OfApplication(ADMINISTRATION), APPLICATION_SCOPED_ROLE)))
                .expectNextCount(1)
                .verifyComplete();
        assertThat(applicationGateCalls).containsExactly(ADMINISTRATION);
        assertThat(tenantGateCalls).isEmpty();
    }

    // ---- alcance TENANT (HU-036) --------------------------------------------------------------------------------

    @Test
    void tenant_actor_that_administers_an_application_defines_the_role_and_audits_allowed_under_that_application() {
        List<DefineRoleRequest> received = new ArrayList<>();
        List<AdministrationEvent> audited = new ArrayList<>();
        AdministerRoleDefinitionUseCaseImpl useCase = new AdministerRoleDefinitionUseCaseImpl(
                neverAskedAboutAnApplication(), tenantAllows(), defineRoleCapturing(received),
                TestAdministrationAuditRepositories.capturing(audited), () -> FIXED_UUID, () -> FIXED_INSTANT);

        StepVerifier.create(useCase.execute(new AdministerRoleDefinitionRequest(
                        new CatalogAdministration.OfTenant(TENANT_ACTOR), TENANT_SCOPED_ROLE)))
                .assertNext(response -> assertThat(response).isEqualTo(RESPONSE))
                .verifyComplete();
        assertThat(tenantGateCalls).containsExactly(TENANT_ACTOR);
        assertThat(received).containsExactly(TENANT_SCOPED_ROLE);
        assertThat(audited).containsExactly(new AdministrationEvent(FIXED_UUID, FIXED_UUID.toString(), TENANT,
                GRANTING_APPLICATION, "test-subject", AdministrationOperation.ROLE_DEFINED,
                AdministrationOutcome.ALLOWED, FIXED_INSTANT));
    }

    @Test
    void tenant_gate_denial_never_reaches_define_role() {
        DefineRoleUseCase defineRole = input -> {
            throw new AssertionError("must not reach DefineRoleUseCase");
        };
        AdministerRoleDefinitionUseCaseImpl useCase = new AdministerRoleDefinitionUseCaseImpl(
                neverAskedAboutAnApplication(), tenantDeniesAfterEvaluating(FIRST_EVALUATED_APPLICATION), defineRole,
                TestAdministrationAuditRepositories.capturing(new ArrayList<>()), () -> FIXED_UUID, () -> FIXED_INSTANT);

        StepVerifier.create(useCase.execute(new AdministerRoleDefinitionRequest(
                        new CatalogAdministration.OfTenant(TENANT_ACTOR), TENANT_SCOPED_ROLE)))
                .expectError(NotAuthorizedToAdministerTenantCatalogException.class)
                .verify();
    }

    @Test
    void tenant_denial_audits_denied_under_the_first_evaluated_application() {
        List<AdministrationEvent> audited = new ArrayList<>();
        AdministerRoleDefinitionUseCaseImpl useCase = new AdministerRoleDefinitionUseCaseImpl(
                neverAskedAboutAnApplication(), tenantDeniesAfterEvaluating(FIRST_EVALUATED_APPLICATION),
                defineRoleCapturing(new ArrayList<>()), TestAdministrationAuditRepositories.capturing(audited),
                () -> FIXED_UUID, () -> FIXED_INSTANT);

        StepVerifier.create(useCase.execute(new AdministerRoleDefinitionRequest(
                        new CatalogAdministration.OfTenant(TENANT_ACTOR), TENANT_SCOPED_ROLE)))
                .expectError(NotAuthorizedToAdministerTenantCatalogException.class)
                .verify();
        assertThat(audited).containsExactly(new AdministrationEvent(FIXED_UUID, FIXED_UUID.toString(), TENANT,
                FIRST_EVALUATED_APPLICATION, "test-subject", AdministrationOperation.ROLE_DEFINED,
                AdministrationOutcome.DENIED, FIXED_INSTANT));
    }

    @Test
    void tenant_denial_without_an_evaluated_application_audits_nothing() {
        List<AdministrationEvent> audited = new ArrayList<>();
        PrincipalMustAdministerAtLeastOneApplicationValidator noApplications = actor -> Mono.error(
                new NotAuthorizedToAdministerTenantCatalogException(TENANT, Optional.empty()));
        AdministerRoleDefinitionUseCaseImpl useCase = new AdministerRoleDefinitionUseCaseImpl(
                neverAskedAboutAnApplication(), noApplications, defineRoleCapturing(new ArrayList<>()),
                TestAdministrationAuditRepositories.capturing(audited), () -> FIXED_UUID, () -> FIXED_INSTANT);

        StepVerifier.create(useCase.execute(new AdministerRoleDefinitionRequest(
                        new CatalogAdministration.OfTenant(TENANT_ACTOR), TENANT_SCOPED_ROLE)))
                .expectError(NotAuthorizedToAdministerTenantCatalogException.class)
                .verify();
        assertThat(audited).isEmpty();
    }

    @Test
    void tenant_mfa_requirement_propagates_without_audit() {
        List<AdministrationEvent> audited = new ArrayList<>();
        PrincipalMustAdministerAtLeastOneApplicationValidator needsMfa = actor -> Mono.error(
                new MfaEvidenceRequiredException(TENANT, FIRST_EVALUATED_APPLICATION));
        AdministerRoleDefinitionUseCaseImpl useCase = new AdministerRoleDefinitionUseCaseImpl(
                neverAskedAboutAnApplication(), needsMfa, defineRoleCapturing(new ArrayList<>()),
                TestAdministrationAuditRepositories.capturing(audited), () -> FIXED_UUID, () -> FIXED_INSTANT);

        StepVerifier.create(useCase.execute(new AdministerRoleDefinitionRequest(
                        new CatalogAdministration.OfTenant(TENANT_ACTOR), TENANT_SCOPED_ROLE)))
                .expectError(MfaEvidenceRequiredException.class)
                .verify();
        assertThat(audited).isEmpty();
    }

    @Test
    void a_failing_audit_does_not_break_the_tenant_definition() {
        AdministerRoleDefinitionUseCaseImpl useCase = new AdministerRoleDefinitionUseCaseImpl(
                neverAskedAboutAnApplication(), tenantAllows(), defineRoleCapturing(new ArrayList<>()),
                TestAdministrationAuditRepositories.failing(), () -> FIXED_UUID, () -> FIXED_INSTANT);

        StepVerifier.create(useCase.execute(new AdministerRoleDefinitionRequest(
                        new CatalogAdministration.OfTenant(TENANT_ACTOR), TENANT_SCOPED_ROLE)))
                .assertNext(response -> assertThat(response).isEqualTo(RESPONSE))
                .verifyComplete();
    }

    @Test
    void tenant_definition_that_fails_after_the_gate_is_not_audited() {
        List<AdministrationEvent> audited = new ArrayList<>();
        DefineRoleUseCase failing = input -> Mono.error(new IllegalStateException("el rol ya existe"));
        AdministerRoleDefinitionUseCaseImpl useCase = new AdministerRoleDefinitionUseCaseImpl(
                neverAskedAboutAnApplication(), tenantAllows(), failing,
                TestAdministrationAuditRepositories.capturing(audited), () -> FIXED_UUID, () -> FIXED_INSTANT);

        StepVerifier.create(useCase.execute(new AdministerRoleDefinitionRequest(
                        new CatalogAdministration.OfTenant(TENANT_ACTOR), TENANT_SCOPED_ROLE)))
                .expectError(IllegalStateException.class)
                .verify();
        assertThat(audited).isEmpty();
    }

    @Test
    void the_application_gate_is_not_invoked_for_a_tenant_role() {
        AdministerRoleDefinitionUseCaseImpl useCase = new AdministerRoleDefinitionUseCaseImpl(
                allows(), tenantAllows(), defineRoleCapturing(new ArrayList<>()),
                TestAdministrationAuditRepositories.capturing(new ArrayList<>()), () -> FIXED_UUID, () -> FIXED_INSTANT);

        StepVerifier.create(useCase.execute(new AdministerRoleDefinitionRequest(
                        new CatalogAdministration.OfTenant(TENANT_ACTOR), TENANT_SCOPED_ROLE)))
                .expectNextCount(1)
                .verifyComplete();
        assertThat(applicationGateCalls).isEmpty();
        assertThat(tenantGateCalls).containsExactly(TENANT_ACTOR);
    }

    // ---- dobles ---------------------------------------------------------------------------------------------------

    private PrincipalMustBeApplicationAdministratorValidator allows() {
        return request -> Mono.defer(() -> {
            applicationGateCalls.add(request);
            return Mono.empty();
        });
    }

    private static PrincipalMustBeApplicationAdministratorValidator denies() {
        return request -> Mono.error(new NotAuthorizedToAdministerException(TENANT, APPLICATION));
    }

    private static PrincipalMustBeApplicationAdministratorValidator neverAskedAboutAnApplication() {
        return request -> {
            throw new AssertionError("must not reach the application administration gate");
        };
    }

    private PrincipalMustAdministerAtLeastOneApplicationValidator tenantAllows() {
        return actor -> Mono.defer(() -> {
            tenantGateCalls.add(actor);
            return Mono.just(GRANTING_APPLICATION);
        });
    }

    private static PrincipalMustAdministerAtLeastOneApplicationValidator tenantDeniesAfterEvaluating(ApplicationId evaluated) {
        return actor -> Mono.error(new NotAuthorizedToAdministerTenantCatalogException(TENANT, Optional.of(evaluated)));
    }

    private static PrincipalMustAdministerAtLeastOneApplicationValidator neverAskedAboutTheTenant() {
        return actor -> {
            throw new AssertionError("must not reach the tenant administration gate");
        };
    }

    private static DefineRoleUseCase defineRoleCapturing(List<DefineRoleRequest> received) {
        return input -> {
            received.add(input);
            return Mono.just(RESPONSE);
        };
    }
}
