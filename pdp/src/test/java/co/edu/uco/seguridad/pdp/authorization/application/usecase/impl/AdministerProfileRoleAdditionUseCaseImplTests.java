package co.edu.uco.seguridad.pdp.authorization.application.usecase.impl;

import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AdministerProfileRoleAdditionRequest;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AdministrationRequest;
import co.edu.uco.seguridad.pdp.authorization.application.rule.validator.PrincipalMustBeApplicationAdministratorValidator;
import co.edu.uco.seguridad.pdp.authorization.domain.exception.NotAuthorizedToAdministerException;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.ProfileId;
import co.edu.uco.seguridad.pdp.commons.model.RoleId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.commons.model.UserId;
import co.edu.uco.seguridad.pdp.profiles.application.primaryport.request.AddRoleToProfileRequest;
import co.edu.uco.seguridad.pdp.profiles.application.primaryport.response.ProfileResponse;
import co.edu.uco.seguridad.pdp.profiles.application.usecase.AddRoleToProfileUseCase;
import co.edu.uco.seguridad.pdp.profiles.domain.model.ProfileName;
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
 * HU-019: gatea AddRoleToProfileUseCase solo cuando hay administración que exigir (perfil de la
 * adición con alcance APPLICATION) — un perfil TENANT (administration vacío) delega directo.
 */
class AdministerProfileRoleAdditionUseCaseImplTests {

    private static final TenantId TENANT = new TenantId("universidad-uco");
    private static final ApplicationId APPLICATION = new ApplicationId(UUID.randomUUID());
    private static final UserId USER = new UserId(UUID.randomUUID());
    private static final ProfileId PROFILE = new ProfileId(UUID.randomUUID());
    private static final RoleId ROLE = new RoleId(UUID.randomUUID());
    private static final AdministrationRequest ADMINISTRATION =
            new AdministrationRequest(TENANT, APPLICATION, USER, "test-subject", Set.of());
    private static final AddRoleToProfileRequest ADDITION = new AddRoleToProfileRequest(TENANT, PROFILE, ROLE);
    private static final ProfileResponse RESPONSE = new ProfileResponse(PROFILE, new ProfileName("Docentes de matematicas"),
            RoleScope.ofApplication(TENANT, APPLICATION), Set.of(ROLE), Instant.parse("2026-09-15T00:00:00Z"));
    private static final UUID FIXED_UUID = UUID.randomUUID();
    private static final Instant FIXED_INSTANT = Instant.parse("2026-09-15T00:00:00Z");

    @Test
    void adds_the_role_when_the_principal_administers_the_application_and_audits_allowed() {
        List<AddRoleToProfileRequest> received = new ArrayList<>();
        List<AdministrationEvent> audited = new ArrayList<>();
        AdministerProfileRoleAdditionUseCaseImpl useCase = new AdministerProfileRoleAdditionUseCaseImpl(
                allows(), addRoleCapturing(received), TestAdministrationAuditRepositories.capturing(audited),
                () -> FIXED_UUID, () -> FIXED_INSTANT);

        StepVerifier.create(useCase.execute(new AdministerProfileRoleAdditionRequest(Optional.of(ADMINISTRATION), ADDITION)))
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
                denies(), addRoleToProfile, TestAdministrationAuditRepositories.capturing(audited), () -> FIXED_UUID,
                () -> FIXED_INSTANT);

        StepVerifier.create(useCase.execute(new AdministerProfileRoleAdditionRequest(Optional.of(ADMINISTRATION), ADDITION)))
                .expectError(NotAuthorizedToAdministerException.class)
                .verify();
        assertThat(audited).hasSize(1);
        assertThat(audited.getFirst().outcome()).isEqualTo(AdministrationOutcome.DENIED);
    }

    @Test
    void adds_a_role_to_a_tenant_scoped_profile_without_gating_or_auditing_when_administration_is_empty() {
        List<AddRoleToProfileRequest> received = new ArrayList<>();
        PrincipalMustBeApplicationAdministratorValidator neverCalled = request -> {
            throw new AssertionError("must not reach the administration gate");
        };
        AdministerProfileRoleAdditionUseCaseImpl useCase = new AdministerProfileRoleAdditionUseCaseImpl(
                neverCalled, addRoleCapturing(received), TestAdministrationAuditRepositories.unreachable(),
                () -> FIXED_UUID, () -> FIXED_INSTANT);

        StepVerifier.create(useCase.execute(new AdministerProfileRoleAdditionRequest(Optional.empty(), ADDITION)))
                .assertNext(response -> assertThat(response).isEqualTo(RESPONSE))
                .verifyComplete();
        assertThat(received).containsExactly(ADDITION);
    }

    @Test
    void does_not_block_the_result_when_the_audit_repository_fails() {
        List<AddRoleToProfileRequest> received = new ArrayList<>();
        AdministerProfileRoleAdditionUseCaseImpl useCase = new AdministerProfileRoleAdditionUseCaseImpl(
                allows(), addRoleCapturing(received), TestAdministrationAuditRepositories.failing(), () -> FIXED_UUID,
                () -> FIXED_INSTANT);

        StepVerifier.create(useCase.execute(new AdministerProfileRoleAdditionRequest(Optional.of(ADMINISTRATION), ADDITION)))
                .assertNext(response -> assertThat(response).isEqualTo(RESPONSE))
                .verifyComplete();
    }

    private static PrincipalMustBeApplicationAdministratorValidator allows() {
        return request -> Mono.empty();
    }

    private static PrincipalMustBeApplicationAdministratorValidator denies() {
        return request -> Mono.error(new NotAuthorizedToAdministerException(TENANT, APPLICATION));
    }

    private static AddRoleToProfileUseCase addRoleCapturing(List<AddRoleToProfileRequest> received) {
        return input -> {
            received.add(input);
            return Mono.just(RESPONSE);
        };
    }
}
