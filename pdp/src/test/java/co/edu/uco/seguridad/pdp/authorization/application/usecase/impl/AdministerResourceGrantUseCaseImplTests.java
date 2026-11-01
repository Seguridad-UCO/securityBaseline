package co.edu.uco.seguridad.pdp.authorization.application.usecase.impl;

import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AdministerResourceGrantRequest;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AdministrationRequest;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.CatalogAdministration;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.TenantAdministrationRequest;
import co.edu.uco.seguridad.pdp.authorization.application.rule.validator.PrincipalMustAdministerAtLeastOneApplicationValidator;
import co.edu.uco.seguridad.pdp.authorization.application.rule.validator.PrincipalMustBeApplicationAdministratorValidator;
import co.edu.uco.seguridad.pdp.authorization.domain.exception.MfaEvidenceRequiredException;
import co.edu.uco.seguridad.pdp.authorization.domain.exception.NotAuthorizedToAdministerException;
import co.edu.uco.seguridad.pdp.authorization.domain.exception.NotAuthorizedToAdministerTenantCatalogException;
import co.edu.uco.seguridad.pdp.commons.model.*;
import co.edu.uco.seguridad.pdp.roles.application.primaryport.request.GrantResourceRequest;
import co.edu.uco.seguridad.pdp.roles.application.primaryport.request.RevokeResourceRequest;
import co.edu.uco.seguridad.pdp.roles.application.primaryport.response.RoleResponse;
import co.edu.uco.seguridad.pdp.roles.application.usecase.GrantResourceToRoleUseCase;
import co.edu.uco.seguridad.pdp.roles.application.usecase.RevokeResourceFromRoleUseCase;
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
 * HU-016 + HU-036: gatea GrantResourceToRoleUseCase / RevokeResourceFromRoleUseCase según el alcance del rol
 * que recibe el recurso. Un rol {@code APPLICATION} exige administrar esa aplicación; un rol {@code TENANT}
 * exige administrar al menos una aplicación del inquilino (editar el rol afecta a todo el inquilino, igual que
 * crearlo) — ya no existe el camino «sin gate». Audita {@code RESOURCE_GRANTED}/{@code RESOURCE_REVOKED}.
 */
class AdministerResourceGrantUseCaseImplTests {

    private static final TenantId TENANT = new TenantId("universidad-uco");
    private static final ApplicationId APPLICATION = new ApplicationId(UUID.randomUUID());
    private static final ApplicationId GRANTING_APPLICATION = new ApplicationId(UUID.fromString("00000000-0000-0000-0000-0000000000a2"));
    private static final ApplicationId FIRST_EVALUATED_APPLICATION = new ApplicationId(UUID.fromString("00000000-0000-0000-0000-0000000000a1"));
    private static final UserId USER = new UserId(UUID.randomUUID());
    private static final RoleId ROLE = new RoleId(UUID.randomUUID());
    private static final ResourceId RESOURCE = new ResourceId(UUID.randomUUID());
    private static final AdministrationRequest ADMINISTRATION =
            new AdministrationRequest(TENANT, APPLICATION, USER, "test-subject", Set.of());
    private static final TenantAdministrationRequest TENANT_ACTOR =
            new TenantAdministrationRequest(TENANT, USER, "test-subject", AuthenticationContextEvidence.NONE);
    private static final GrantResourceRequest GRANT = new GrantResourceRequest(TENANT, ROLE, RESOURCE);
    private static final RevokeResourceRequest REVOCATION = new RevokeResourceRequest(TENANT, ROLE, RESOURCE);
    private static final RoleResponse RESPONSE = new RoleResponse(ROLE, new RoleName("Docente"),
            RoleScope.ofApplication(TENANT, APPLICATION), Set.of(RESOURCE), Instant.parse("2026-09-14T00:00:00Z"));
    private static final UUID FIXED_UUID = UUID.randomUUID();
    private static final Instant FIXED_INSTANT = Instant.parse("2026-09-15T00:00:00Z");

    private final List<AdministrationRequest> applicationGateCalls = new ArrayList<>();
    private final List<TenantAdministrationRequest> tenantGateCalls = new ArrayList<>();

    // ---- alcance APPLICATION (sin regresión, C13) ----------------------------------------------------------------

    @Test
    void grants_the_resource_when_the_principal_administers_the_application_and_audits_allowed() {
        List<GrantResourceRequest> received = new ArrayList<>();
        List<AdministrationEvent> audited = new ArrayList<>();
        AdministerResourceGrantUseCaseImpl useCase = new AdministerResourceGrantUseCaseImpl(
                allows(), neverAskedAboutTheTenant(), grantResourceCapturing(received),
                TestAdministrationAuditRepositories.capturing(audited), () -> FIXED_UUID, () -> FIXED_INSTANT);

        StepVerifier.create(useCase.execute(new AdministerResourceGrantRequest(
                        new CatalogAdministration.OfApplication(ADMINISTRATION), GRANT)))
                .assertNext(response -> assertThat(response).isEqualTo(RESPONSE))
                .verifyComplete();
        assertThat(received).containsExactly(GRANT);
        assertThat(audited).hasSize(1);
        assertThat(audited.getFirst().operation()).isEqualTo(AdministrationOperation.RESOURCE_GRANTED);
        assertThat(audited.getFirst().outcome()).isEqualTo(AdministrationOutcome.ALLOWED);
    }

    @Test
    void never_grants_the_resource_when_the_principal_does_not_administer_the_application_and_audits_denied() {
        GrantResourceToRoleUseCase grantResource = input -> {
            throw new AssertionError("must not reach GrantResourceToRoleUseCase");
        };
        List<AdministrationEvent> audited = new ArrayList<>();
        AdministerResourceGrantUseCaseImpl useCase = new AdministerResourceGrantUseCaseImpl(denies(),
                neverAskedAboutTheTenant(), grantResource, TestAdministrationAuditRepositories.capturing(audited),
                () -> FIXED_UUID, () -> FIXED_INSTANT);

        StepVerifier.create(useCase.execute(new AdministerResourceGrantRequest(
                        new CatalogAdministration.OfApplication(ADMINISTRATION), GRANT)))
                .expectError(NotAuthorizedToAdministerException.class)
                .verify();
        assertThat(audited).hasSize(1);
        assertThat(audited.getFirst().outcome()).isEqualTo(AdministrationOutcome.DENIED);
    }

    @Test
    void does_not_block_the_result_when_the_audit_repository_fails() {
        AdministerResourceGrantUseCaseImpl useCase = new AdministerResourceGrantUseCaseImpl(
                allows(), neverAskedAboutTheTenant(), grantResourceCapturing(new ArrayList<>()),
                TestAdministrationAuditRepositories.failing(), () -> FIXED_UUID, () -> FIXED_INSTANT);

        StepVerifier.create(useCase.execute(new AdministerResourceGrantRequest(
                        new CatalogAdministration.OfApplication(ADMINISTRATION), GRANT)))
                .assertNext(response -> assertThat(response).isEqualTo(RESPONSE))
                .verifyComplete();
    }

    @Test
    void the_tenant_gate_is_not_invoked_for_an_application_role() {
        AdministerResourceGrantUseCaseImpl useCase = new AdministerResourceGrantUseCaseImpl(
                allows(), tenantAllows(), grantResourceCapturing(new ArrayList<>()),
                TestAdministrationAuditRepositories.capturing(new ArrayList<>()), () -> FIXED_UUID, () -> FIXED_INSTANT);

        StepVerifier.create(useCase.execute(new AdministerResourceGrantRequest(
                        new CatalogAdministration.OfApplication(ADMINISTRATION), GRANT)))
                .expectNextCount(1)
                .verifyComplete();
        assertThat(applicationGateCalls).containsExactly(ADMINISTRATION);
        assertThat(tenantGateCalls).isEmpty();
    }

    // ---- alcance TENANT (HU-036) --------------------------------------------------------------------------------

    @Test
    void tenant_grant_is_gated_and_audited_as_resource_granted() {
        List<GrantResourceRequest> received = new ArrayList<>();
        List<AdministrationEvent> audited = new ArrayList<>();
        AdministerResourceGrantUseCaseImpl useCase = new AdministerResourceGrantUseCaseImpl(
                neverAskedAboutAnApplication(), tenantAllows(), grantResourceCapturing(received), revokeNeverReached(),
                TestAdministrationAuditRepositories.capturing(audited), () -> FIXED_UUID, () -> FIXED_INSTANT);

        StepVerifier.create(useCase.execute(new AdministerResourceGrantRequest(
                        new CatalogAdministration.OfTenant(TENANT_ACTOR), GRANT)))
                .assertNext(response -> assertThat(response).isEqualTo(RESPONSE))
                .verifyComplete();
        assertThat(tenantGateCalls).containsExactly(TENANT_ACTOR);
        assertThat(received).containsExactly(GRANT);
        assertThat(audited).containsExactly(new AdministrationEvent(FIXED_UUID, FIXED_UUID.toString(), TENANT,
                GRANTING_APPLICATION, "test-subject", AdministrationOperation.RESOURCE_GRANTED,
                AdministrationOutcome.ALLOWED, FIXED_INSTANT));
    }

    @Test
    void tenant_revocation_is_gated_and_audited_as_resource_revoked() {
        List<RevokeResourceRequest> received = new ArrayList<>();
        List<AdministrationEvent> audited = new ArrayList<>();
        AdministerResourceGrantUseCaseImpl useCase = new AdministerResourceGrantUseCaseImpl(
                neverAskedAboutAnApplication(), tenantAllows(), grantNeverReached(), revokeCapturing(received),
                TestAdministrationAuditRepositories.capturing(audited), () -> FIXED_UUID, () -> FIXED_INSTANT);

        StepVerifier.create(useCase.execute(new AdministerResourceGrantRequest(
                        new CatalogAdministration.OfTenant(TENANT_ACTOR), GRANT, true)))
                .assertNext(response -> assertThat(response).isEqualTo(RESPONSE))
                .verifyComplete();
        assertThat(tenantGateCalls).containsExactly(TENANT_ACTOR);
        assertThat(received).containsExactly(REVOCATION);
        assertThat(audited).containsExactly(new AdministrationEvent(FIXED_UUID, FIXED_UUID.toString(), TENANT,
                GRANTING_APPLICATION, "test-subject", AdministrationOperation.RESOURCE_REVOKED,
                AdministrationOutcome.ALLOWED, FIXED_INSTANT));
    }

    @Test
    void tenant_denial_never_reaches_grant_or_revoke() {
        AdministerResourceGrantUseCaseImpl useCase = new AdministerResourceGrantUseCaseImpl(
                neverAskedAboutAnApplication(), tenantDeniesAfterEvaluating(FIRST_EVALUATED_APPLICATION),
                grantNeverReached(), revokeNeverReached(),
                TestAdministrationAuditRepositories.capturing(new ArrayList<>()), () -> FIXED_UUID, () -> FIXED_INSTANT);

        StepVerifier.create(useCase.execute(new AdministerResourceGrantRequest(
                        new CatalogAdministration.OfTenant(TENANT_ACTOR), GRANT)))
                .expectError(NotAuthorizedToAdministerTenantCatalogException.class)
                .verify();
        StepVerifier.create(useCase.execute(new AdministerResourceGrantRequest(
                        new CatalogAdministration.OfTenant(TENANT_ACTOR), GRANT, true)))
                .expectError(NotAuthorizedToAdministerTenantCatalogException.class)
                .verify();
    }

    @Test
    void tenant_denial_audits_denied_under_the_first_evaluated_application_with_the_operation_of_the_request() {
        List<AdministrationEvent> audited = new ArrayList<>();
        AdministerResourceGrantUseCaseImpl useCase = new AdministerResourceGrantUseCaseImpl(
                neverAskedAboutAnApplication(), tenantDeniesAfterEvaluating(FIRST_EVALUATED_APPLICATION),
                grantNeverReached(), revokeNeverReached(),
                TestAdministrationAuditRepositories.capturing(audited), () -> FIXED_UUID, () -> FIXED_INSTANT);

        StepVerifier.create(useCase.execute(new AdministerResourceGrantRequest(
                        new CatalogAdministration.OfTenant(TENANT_ACTOR), GRANT)))
                .expectError(NotAuthorizedToAdministerTenantCatalogException.class)
                .verify();
        StepVerifier.create(useCase.execute(new AdministerResourceGrantRequest(
                        new CatalogAdministration.OfTenant(TENANT_ACTOR), GRANT, true)))
                .expectError(NotAuthorizedToAdministerTenantCatalogException.class)
                .verify();
        assertThat(audited).containsExactly(
                new AdministrationEvent(FIXED_UUID, FIXED_UUID.toString(), TENANT, FIRST_EVALUATED_APPLICATION,
                        "test-subject", AdministrationOperation.RESOURCE_GRANTED, AdministrationOutcome.DENIED, FIXED_INSTANT),
                new AdministrationEvent(FIXED_UUID, FIXED_UUID.toString(), TENANT, FIRST_EVALUATED_APPLICATION,
                        "test-subject", AdministrationOperation.RESOURCE_REVOKED, AdministrationOutcome.DENIED, FIXED_INSTANT));
    }

    @Test
    void tenant_denial_without_an_evaluated_application_audits_nothing() {
        List<AdministrationEvent> audited = new ArrayList<>();
        PrincipalMustAdministerAtLeastOneApplicationValidator noApplications = actor -> Mono.error(
                new NotAuthorizedToAdministerTenantCatalogException(TENANT, Optional.empty()));
        AdministerResourceGrantUseCaseImpl useCase = new AdministerResourceGrantUseCaseImpl(
                neverAskedAboutAnApplication(), noApplications, grantNeverReached(), revokeNeverReached(),
                TestAdministrationAuditRepositories.capturing(audited), () -> FIXED_UUID, () -> FIXED_INSTANT);

        StepVerifier.create(useCase.execute(new AdministerResourceGrantRequest(
                        new CatalogAdministration.OfTenant(TENANT_ACTOR), GRANT)))
                .expectError(NotAuthorizedToAdministerTenantCatalogException.class)
                .verify();
        assertThat(audited).isEmpty();
    }

    @Test
    void tenant_mfa_requirement_propagates_without_audit() {
        List<AdministrationEvent> audited = new ArrayList<>();
        PrincipalMustAdministerAtLeastOneApplicationValidator needsMfa = actor -> Mono.error(
                new MfaEvidenceRequiredException(TENANT, FIRST_EVALUATED_APPLICATION));
        AdministerResourceGrantUseCaseImpl useCase = new AdministerResourceGrantUseCaseImpl(
                neverAskedAboutAnApplication(), needsMfa, grantNeverReached(), revokeNeverReached(),
                TestAdministrationAuditRepositories.capturing(audited), () -> FIXED_UUID, () -> FIXED_INSTANT);

        StepVerifier.create(useCase.execute(new AdministerResourceGrantRequest(
                        new CatalogAdministration.OfTenant(TENANT_ACTOR), GRANT)))
                .expectError(MfaEvidenceRequiredException.class)
                .verify();
        assertThat(audited).isEmpty();
    }

    @Test
    void a_failing_audit_does_not_break_the_tenant_grant() {
        AdministerResourceGrantUseCaseImpl useCase = new AdministerResourceGrantUseCaseImpl(
                neverAskedAboutAnApplication(), tenantAllows(), grantResourceCapturing(new ArrayList<>()),
                TestAdministrationAuditRepositories.failing(), () -> FIXED_UUID, () -> FIXED_INSTANT);

        StepVerifier.create(useCase.execute(new AdministerResourceGrantRequest(
                        new CatalogAdministration.OfTenant(TENANT_ACTOR), GRANT)))
                .assertNext(response -> assertThat(response).isEqualTo(RESPONSE))
                .verifyComplete();
    }

    @Test
    void tenant_grant_that_fails_after_the_gate_is_not_audited() {
        List<AdministrationEvent> audited = new ArrayList<>();
        GrantResourceToRoleUseCase failing = input -> Mono.error(new IllegalStateException("el recurso ya está concedido"));
        AdministerResourceGrantUseCaseImpl useCase = new AdministerResourceGrantUseCaseImpl(
                neverAskedAboutAnApplication(), tenantAllows(), failing,
                TestAdministrationAuditRepositories.capturing(audited), () -> FIXED_UUID, () -> FIXED_INSTANT);

        StepVerifier.create(useCase.execute(new AdministerResourceGrantRequest(
                        new CatalogAdministration.OfTenant(TENANT_ACTOR), GRANT)))
                .expectError(IllegalStateException.class)
                .verify();
        assertThat(audited).isEmpty();
    }

    @Test
    void the_application_gate_is_not_invoked_for_a_tenant_role() {
        AdministerResourceGrantUseCaseImpl useCase = new AdministerResourceGrantUseCaseImpl(
                allows(), tenantAllows(), grantResourceCapturing(new ArrayList<>()),
                TestAdministrationAuditRepositories.capturing(new ArrayList<>()), () -> FIXED_UUID, () -> FIXED_INSTANT);

        StepVerifier.create(useCase.execute(new AdministerResourceGrantRequest(
                        new CatalogAdministration.OfTenant(TENANT_ACTOR), GRANT)))
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

    private static GrantResourceToRoleUseCase grantResourceCapturing(List<GrantResourceRequest> received) {
        return input -> {
            received.add(input);
            return Mono.just(RESPONSE);
        };
    }

    private static GrantResourceToRoleUseCase grantNeverReached() {
        return input -> {
            throw new AssertionError("must not reach GrantResourceToRoleUseCase");
        };
    }

    private static RevokeResourceFromRoleUseCase revokeCapturing(List<RevokeResourceRequest> received) {
        return input -> {
            received.add(input);
            return Mono.just(RESPONSE);
        };
    }

    private static RevokeResourceFromRoleUseCase revokeNeverReached() {
        return input -> {
            throw new AssertionError("must not reach RevokeResourceFromRoleUseCase");
        };
    }
}
