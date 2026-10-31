package co.edu.uco.seguridad.pdp.authorization.application.usecase.impl;

import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AdministerProfileRoleAdditionRequest;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AdministrationRequest;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.CatalogAdministration;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.TenantAdministrationRequest;
import co.edu.uco.seguridad.pdp.authorization.application.rule.validator.PrincipalMustAdministerAtLeastOneApplicationValidator;
import co.edu.uco.seguridad.pdp.authorization.application.rule.validator.PrincipalMustBeApplicationAdministratorValidator;
import co.edu.uco.seguridad.pdp.authorization.domain.exception.MfaEvidenceRequiredException;
import co.edu.uco.seguridad.pdp.authorization.domain.exception.NotAuthorizedToAdministerException;
import co.edu.uco.seguridad.pdp.authorization.domain.exception.NotAuthorizedToAdministerTenantCatalogException;
import co.edu.uco.seguridad.pdp.commons.model.*;
import co.edu.uco.seguridad.pdp.profiles.application.primaryport.request.AddRoleToProfileRequest;
import co.edu.uco.seguridad.pdp.profiles.application.primaryport.request.RemoveRoleFromProfileRequest;
import co.edu.uco.seguridad.pdp.profiles.application.primaryport.response.ProfileResponse;
import co.edu.uco.seguridad.pdp.profiles.application.usecase.AddRoleToProfileUseCase;
import co.edu.uco.seguridad.pdp.profiles.application.usecase.RemoveRoleFromProfileUseCase;
import co.edu.uco.seguridad.pdp.profiles.domain.model.ProfileName;
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
 * HU-019 + HU-036: gatea AddRoleToProfileUseCase / RemoveRoleFromProfileUseCase según el alcance del perfil
 * editado. Un perfil {@code APPLICATION} exige administrar esa aplicación; un perfil {@code TENANT} exige
 * administrar al menos una aplicación del inquilino — ya no existe el camino «sin gate». Audita
 * {@code PROFILE_ROLE_ADDED}/{@code PROFILE_ROLE_REMOVED}.
 */
class AdministerProfileRoleAdditionUseCaseImplTests {

    private static final TenantId TENANT = new TenantId("universidad-uco");
    private static final ApplicationId APPLICATION = new ApplicationId(UUID.randomUUID());
    private static final ApplicationId GRANTING_APPLICATION = new ApplicationId(UUID.fromString("00000000-0000-0000-0000-0000000000a2"));
    private static final ApplicationId FIRST_EVALUATED_APPLICATION = new ApplicationId(UUID.fromString("00000000-0000-0000-0000-0000000000a1"));
    private static final UserId USER = new UserId(UUID.randomUUID());
    private static final ProfileId PROFILE = new ProfileId(UUID.randomUUID());
    private static final RoleId ROLE = new RoleId(UUID.randomUUID());
    private static final AdministrationRequest ADMINISTRATION =
            new AdministrationRequest(TENANT, APPLICATION, USER, "test-subject", Set.of());
    private static final TenantAdministrationRequest TENANT_ACTOR =
            new TenantAdministrationRequest(TENANT, USER, "test-subject", AuthenticationContextEvidence.NONE);
    private static final AddRoleToProfileRequest ADDITION = new AddRoleToProfileRequest(TENANT, PROFILE, ROLE);
    private static final RemoveRoleFromProfileRequest REMOVAL = new RemoveRoleFromProfileRequest(TENANT, PROFILE, ROLE);
    private static final ProfileResponse RESPONSE = new ProfileResponse(PROFILE, new ProfileName("Docentes de matematicas"),
            RoleScope.ofApplication(TENANT, APPLICATION), Set.of(ROLE), Instant.parse("2026-09-15T00:00:00Z"));
    private static final UUID FIXED_UUID = UUID.randomUUID();
    private static final Instant FIXED_INSTANT = Instant.parse("2026-09-15T00:00:00Z");

    private final List<AdministrationRequest> applicationGateCalls = new ArrayList<>();
    private final List<TenantAdministrationRequest> tenantGateCalls = new ArrayList<>();

    // ---- alcance APPLICATION (sin regresión, C13) ----------------------------------------------------------------

    @Test
    void adds_the_role_when_the_principal_administers_the_application_and_audits_allowed() {
        List<AddRoleToProfileRequest> received = new ArrayList<>();
        List<AdministrationEvent> audited = new ArrayList<>();
        AdministerProfileRoleAdditionUseCaseImpl useCase = new AdministerProfileRoleAdditionUseCaseImpl(
                allows(), neverAskedAboutTheTenant(), addRoleCapturing(received),
                TestAdministrationAuditRepositories.capturing(audited), () -> FIXED_UUID, () -> FIXED_INSTANT);

        StepVerifier.create(useCase.execute(new AdministerProfileRoleAdditionRequest(
                        new CatalogAdministration.OfApplication(ADMINISTRATION), ADDITION)))
                .assertNext(response -> assertThat(response).isEqualTo(RESPONSE))
                .verifyComplete();
        assertThat(received).containsExactly(ADDITION);
        assertThat(audited).hasSize(1);
        assertThat(audited.getFirst().operation()).isEqualTo(AdministrationOperation.PROFILE_ROLE_ADDED);
        assertThat(audited.getFirst().outcome()).isEqualTo(AdministrationOutcome.ALLOWED);
    }

    @Test
    void never_adds_the_role_when_the_principal_does_not_administer_the_application_and_audits_denied() {
        AddRoleToProfileUseCase addRoleToProfile = input -> {
            throw new AssertionError("must not reach AddRoleToProfileUseCase");
        };
        List<AdministrationEvent> audited = new ArrayList<>();
        AdministerProfileRoleAdditionUseCaseImpl useCase = new AdministerProfileRoleAdditionUseCaseImpl(
                denies(), neverAskedAboutTheTenant(), addRoleToProfile,
                TestAdministrationAuditRepositories.capturing(audited), () -> FIXED_UUID, () -> FIXED_INSTANT);

        StepVerifier.create(useCase.execute(new AdministerProfileRoleAdditionRequest(
                        new CatalogAdministration.OfApplication(ADMINISTRATION), ADDITION)))
                .expectError(NotAuthorizedToAdministerException.class)
                .verify();
        assertThat(audited).hasSize(1);
        assertThat(audited.getFirst().outcome()).isEqualTo(AdministrationOutcome.DENIED);
    }

    @Test
    void does_not_block_the_result_when_the_audit_repository_fails() {
        AdministerProfileRoleAdditionUseCaseImpl useCase = new AdministerProfileRoleAdditionUseCaseImpl(
                allows(), neverAskedAboutTheTenant(), addRoleCapturing(new ArrayList<>()),
                TestAdministrationAuditRepositories.failing(), () -> FIXED_UUID, () -> FIXED_INSTANT);

        StepVerifier.create(useCase.execute(new AdministerProfileRoleAdditionRequest(
                        new CatalogAdministration.OfApplication(ADMINISTRATION), ADDITION)))
                .assertNext(response -> assertThat(response).isEqualTo(RESPONSE))
                .verifyComplete();
    }

    @Test
    void the_tenant_gate_is_not_invoked_for_an_application_profile() {
        AdministerProfileRoleAdditionUseCaseImpl useCase = new AdministerProfileRoleAdditionUseCaseImpl(
                allows(), tenantAllows(), addRoleCapturing(new ArrayList<>()),
                TestAdministrationAuditRepositories.capturing(new ArrayList<>()), () -> FIXED_UUID, () -> FIXED_INSTANT);

        StepVerifier.create(useCase.execute(new AdministerProfileRoleAdditionRequest(
                        new CatalogAdministration.OfApplication(ADMINISTRATION), ADDITION)))
                .expectNextCount(1)
                .verifyComplete();
        assertThat(applicationGateCalls).containsExactly(ADMINISTRATION);
        assertThat(tenantGateCalls).isEmpty();
    }

    // ---- alcance TENANT (HU-036) --------------------------------------------------------------------------------

    @Test
    void tenant_addition_is_gated_and_audited_as_profile_role_added() {
        List<AddRoleToProfileRequest> received = new ArrayList<>();
        List<AdministrationEvent> audited = new ArrayList<>();
        AdministerProfileRoleAdditionUseCaseImpl useCase = new AdministerProfileRoleAdditionUseCaseImpl(
                neverAskedAboutAnApplication(), tenantAllows(), addRoleCapturing(received), removeNeverReached(),
                TestAdministrationAuditRepositories.capturing(audited), () -> FIXED_UUID, () -> FIXED_INSTANT);

        StepVerifier.create(useCase.execute(new AdministerProfileRoleAdditionRequest(
                        new CatalogAdministration.OfTenant(TENANT_ACTOR), ADDITION)))
                .assertNext(response -> assertThat(response).isEqualTo(RESPONSE))
                .verifyComplete();
        assertThat(tenantGateCalls).containsExactly(TENANT_ACTOR);
        assertThat(received).containsExactly(ADDITION);
        assertThat(audited).containsExactly(new AdministrationEvent(FIXED_UUID, FIXED_UUID.toString(), TENANT,
                GRANTING_APPLICATION, "test-subject", AdministrationOperation.PROFILE_ROLE_ADDED,
                AdministrationOutcome.ALLOWED, FIXED_INSTANT));
    }

    @Test
    void tenant_removal_is_gated_and_audited_as_profile_role_removed() {
        List<RemoveRoleFromProfileRequest> received = new ArrayList<>();
        List<AdministrationEvent> audited = new ArrayList<>();
        AdministerProfileRoleAdditionUseCaseImpl useCase = new AdministerProfileRoleAdditionUseCaseImpl(
                neverAskedAboutAnApplication(), tenantAllows(), addNeverReached(), removeCapturing(received),
                TestAdministrationAuditRepositories.capturing(audited), () -> FIXED_UUID, () -> FIXED_INSTANT);

        StepVerifier.create(useCase.execute(new AdministerProfileRoleAdditionRequest(
                        new CatalogAdministration.OfTenant(TENANT_ACTOR), ADDITION, true)))
                .assertNext(response -> assertThat(response).isEqualTo(RESPONSE))
                .verifyComplete();
        assertThat(tenantGateCalls).containsExactly(TENANT_ACTOR);
        assertThat(received).containsExactly(REMOVAL);
        assertThat(audited).containsExactly(new AdministrationEvent(FIXED_UUID, FIXED_UUID.toString(), TENANT,
                GRANTING_APPLICATION, "test-subject", AdministrationOperation.PROFILE_ROLE_REMOVED,
                AdministrationOutcome.ALLOWED, FIXED_INSTANT));
    }

    @Test
    void tenant_denial_never_reaches_add_or_remove() {
        AdministerProfileRoleAdditionUseCaseImpl useCase = new AdministerProfileRoleAdditionUseCaseImpl(
                neverAskedAboutAnApplication(), tenantDeniesAfterEvaluating(FIRST_EVALUATED_APPLICATION),
                addNeverReached(), removeNeverReached(),
                TestAdministrationAuditRepositories.capturing(new ArrayList<>()), () -> FIXED_UUID, () -> FIXED_INSTANT);

        StepVerifier.create(useCase.execute(new AdministerProfileRoleAdditionRequest(
                        new CatalogAdministration.OfTenant(TENANT_ACTOR), ADDITION)))
                .expectError(NotAuthorizedToAdministerTenantCatalogException.class)
                .verify();
        StepVerifier.create(useCase.execute(new AdministerProfileRoleAdditionRequest(
                        new CatalogAdministration.OfTenant(TENANT_ACTOR), ADDITION, true)))
                .expectError(NotAuthorizedToAdministerTenantCatalogException.class)
                .verify();
    }

    @Test
    void tenant_denial_audits_denied_under_the_first_evaluated_application_with_the_operation_of_the_request() {
        List<AdministrationEvent> audited = new ArrayList<>();
        AdministerProfileRoleAdditionUseCaseImpl useCase = new AdministerProfileRoleAdditionUseCaseImpl(
                neverAskedAboutAnApplication(), tenantDeniesAfterEvaluating(FIRST_EVALUATED_APPLICATION),
                addNeverReached(), removeNeverReached(),
                TestAdministrationAuditRepositories.capturing(audited), () -> FIXED_UUID, () -> FIXED_INSTANT);

        StepVerifier.create(useCase.execute(new AdministerProfileRoleAdditionRequest(
                        new CatalogAdministration.OfTenant(TENANT_ACTOR), ADDITION)))
                .expectError(NotAuthorizedToAdministerTenantCatalogException.class)
                .verify();
        StepVerifier.create(useCase.execute(new AdministerProfileRoleAdditionRequest(
                        new CatalogAdministration.OfTenant(TENANT_ACTOR), ADDITION, true)))
                .expectError(NotAuthorizedToAdministerTenantCatalogException.class)
                .verify();
        assertThat(audited).containsExactly(
                new AdministrationEvent(FIXED_UUID, FIXED_UUID.toString(), TENANT, FIRST_EVALUATED_APPLICATION,
                        "test-subject", AdministrationOperation.PROFILE_ROLE_ADDED, AdministrationOutcome.DENIED, FIXED_INSTANT),
                new AdministrationEvent(FIXED_UUID, FIXED_UUID.toString(), TENANT, FIRST_EVALUATED_APPLICATION,
                        "test-subject", AdministrationOperation.PROFILE_ROLE_REMOVED, AdministrationOutcome.DENIED, FIXED_INSTANT));
    }

    @Test
    void tenant_denial_without_an_evaluated_application_audits_nothing() {
        List<AdministrationEvent> audited = new ArrayList<>();
        PrincipalMustAdministerAtLeastOneApplicationValidator noApplications = actor -> Mono.error(
                new NotAuthorizedToAdministerTenantCatalogException(TENANT, Optional.empty()));
        AdministerProfileRoleAdditionUseCaseImpl useCase = new AdministerProfileRoleAdditionUseCaseImpl(
                neverAskedAboutAnApplication(), noApplications, addNeverReached(), removeNeverReached(),
                TestAdministrationAuditRepositories.capturing(audited), () -> FIXED_UUID, () -> FIXED_INSTANT);

        StepVerifier.create(useCase.execute(new AdministerProfileRoleAdditionRequest(
                        new CatalogAdministration.OfTenant(TENANT_ACTOR), ADDITION)))
                .expectError(NotAuthorizedToAdministerTenantCatalogException.class)
                .verify();
        assertThat(audited).isEmpty();
    }

    @Test
    void tenant_mfa_requirement_propagates_without_audit() {
        List<AdministrationEvent> audited = new ArrayList<>();
        PrincipalMustAdministerAtLeastOneApplicationValidator needsMfa = actor -> Mono.error(
                new MfaEvidenceRequiredException(TENANT, FIRST_EVALUATED_APPLICATION));
        AdministerProfileRoleAdditionUseCaseImpl useCase = new AdministerProfileRoleAdditionUseCaseImpl(
                neverAskedAboutAnApplication(), needsMfa, addNeverReached(), removeNeverReached(),
                TestAdministrationAuditRepositories.capturing(audited), () -> FIXED_UUID, () -> FIXED_INSTANT);

        StepVerifier.create(useCase.execute(new AdministerProfileRoleAdditionRequest(
                        new CatalogAdministration.OfTenant(TENANT_ACTOR), ADDITION)))
                .expectError(MfaEvidenceRequiredException.class)
                .verify();
        assertThat(audited).isEmpty();
    }

    @Test
    void a_failing_audit_does_not_break_the_tenant_addition() {
        AdministerProfileRoleAdditionUseCaseImpl useCase = new AdministerProfileRoleAdditionUseCaseImpl(
                neverAskedAboutAnApplication(), tenantAllows(), addRoleCapturing(new ArrayList<>()),
                TestAdministrationAuditRepositories.failing(), () -> FIXED_UUID, () -> FIXED_INSTANT);

        StepVerifier.create(useCase.execute(new AdministerProfileRoleAdditionRequest(
                        new CatalogAdministration.OfTenant(TENANT_ACTOR), ADDITION)))
                .assertNext(response -> assertThat(response).isEqualTo(RESPONSE))
                .verifyComplete();
    }

    @Test
    void tenant_addition_that_fails_after_the_gate_is_not_audited() {
        List<AdministrationEvent> audited = new ArrayList<>();
        AddRoleToProfileUseCase failing = input -> Mono.error(new IllegalStateException("el rol ya está en el perfil"));
        AdministerProfileRoleAdditionUseCaseImpl useCase = new AdministerProfileRoleAdditionUseCaseImpl(
                neverAskedAboutAnApplication(), tenantAllows(), failing,
                TestAdministrationAuditRepositories.capturing(audited), () -> FIXED_UUID, () -> FIXED_INSTANT);

        StepVerifier.create(useCase.execute(new AdministerProfileRoleAdditionRequest(
                        new CatalogAdministration.OfTenant(TENANT_ACTOR), ADDITION)))
                .expectError(IllegalStateException.class)
                .verify();
        assertThat(audited).isEmpty();
    }

    @Test
    void the_application_gate_is_not_invoked_for_a_tenant_profile() {
        AdministerProfileRoleAdditionUseCaseImpl useCase = new AdministerProfileRoleAdditionUseCaseImpl(
                allows(), tenantAllows(), addRoleCapturing(new ArrayList<>()),
                TestAdministrationAuditRepositories.capturing(new ArrayList<>()), () -> FIXED_UUID, () -> FIXED_INSTANT);

        StepVerifier.create(useCase.execute(new AdministerProfileRoleAdditionRequest(
                        new CatalogAdministration.OfTenant(TENANT_ACTOR), ADDITION)))
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

    private static AddRoleToProfileUseCase addRoleCapturing(List<AddRoleToProfileRequest> received) {
        return input -> {
            received.add(input);
            return Mono.just(RESPONSE);
        };
    }

    private static AddRoleToProfileUseCase addNeverReached() {
        return input -> {
            throw new AssertionError("must not reach AddRoleToProfileUseCase");
        };
    }

    private static RemoveRoleFromProfileUseCase removeCapturing(List<RemoveRoleFromProfileRequest> received) {
        return input -> {
            received.add(input);
            return Mono.just(RESPONSE);
        };
    }

    private static RemoveRoleFromProfileUseCase removeNeverReached() {
        return input -> {
            throw new AssertionError("must not reach RemoveRoleFromProfileUseCase");
        };
    }
}
