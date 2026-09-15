package co.edu.uco.seguridad.pdp.authorization.application.usecase.impl;

import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AdministerResourceGrantRequest;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AdministrationRequest;
import co.edu.uco.seguridad.pdp.authorization.application.rule.validator.PrincipalMustBeApplicationAdministratorValidator;
import co.edu.uco.seguridad.pdp.authorization.domain.exception.NotAuthorizedToAdministerException;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.ResourceId;
import co.edu.uco.seguridad.pdp.commons.model.RoleId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.commons.model.UserId;
import co.edu.uco.seguridad.pdp.roles.application.primaryport.request.GrantResourceRequest;
import co.edu.uco.seguridad.pdp.roles.application.primaryport.response.RoleResponse;
import co.edu.uco.seguridad.pdp.roles.application.usecase.GrantResourceToRoleUseCase;
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
 * HU-016: gatea GrantResourceToRoleUseCase solo cuando hay administración que exigir (rol de la
 * concesión con alcance APPLICATION) — un rol TENANT (administration vacío) delega directo.
 */
class AdministerResourceGrantUseCaseImplTests {

    private static final TenantId TENANT = new TenantId("universidad-uco");
    private static final ApplicationId APPLICATION = new ApplicationId(UUID.randomUUID());
    private static final UserId USER = new UserId(UUID.randomUUID());
    private static final RoleId ROLE = new RoleId(UUID.randomUUID());
    private static final ResourceId RESOURCE = new ResourceId(UUID.randomUUID());
    private static final AdministrationRequest ADMINISTRATION =
            new AdministrationRequest(TENANT, APPLICATION, USER, "test-subject", Set.of());
    private static final GrantResourceRequest GRANT = new GrantResourceRequest(TENANT, ROLE, RESOURCE);
    private static final RoleResponse RESPONSE = new RoleResponse(ROLE, new RoleName("Docente"),
            RoleScope.ofApplication(TENANT, APPLICATION), Set.of(RESOURCE), Instant.parse("2026-09-14T00:00:00Z"));
    private static final UUID FIXED_UUID = UUID.randomUUID();
    private static final Instant FIXED_INSTANT = Instant.parse("2026-09-15T00:00:00Z");

    @Test
    void grants_the_resource_when_the_principal_administers_the_application_and_audits_allowed() {
        List<GrantResourceRequest> received = new ArrayList<>();
        List<AdministrationEvent> audited = new ArrayList<>();
        AdministerResourceGrantUseCaseImpl useCase = new AdministerResourceGrantUseCaseImpl(
                allows(), grantResourceCapturing(received), TestAdministrationAuditRepositories.capturing(audited),
                () -> FIXED_UUID, () -> FIXED_INSTANT);

        StepVerifier.create(useCase.execute(new AdministerResourceGrantRequest(Optional.of(ADMINISTRATION), GRANT)))
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
        AdministerResourceGrantUseCaseImpl useCase = new AdministerResourceGrantUseCaseImpl(denies(), grantResource,
                TestAdministrationAuditRepositories.capturing(audited), () -> FIXED_UUID, () -> FIXED_INSTANT);

        StepVerifier.create(useCase.execute(new AdministerResourceGrantRequest(Optional.of(ADMINISTRATION), GRANT)))
                .expectError(NotAuthorizedToAdministerException.class)
                .verify();
        assertThat(audited).hasSize(1);
        assertThat(audited.getFirst().outcome()).isEqualTo(AdministrationOutcome.DENIED);
    }

    @Test
    void grants_a_resource_to_a_tenant_scoped_role_without_gating_or_auditing_when_administration_is_empty() {
        List<GrantResourceRequest> received = new ArrayList<>();
        PrincipalMustBeApplicationAdministratorValidator neverCalled = request -> {
            throw new AssertionError("must not reach the administration gate");
        };
        AdministerResourceGrantUseCaseImpl useCase = new AdministerResourceGrantUseCaseImpl(
                neverCalled, grantResourceCapturing(received), TestAdministrationAuditRepositories.unreachable(),
                () -> FIXED_UUID, () -> FIXED_INSTANT);

        StepVerifier.create(useCase.execute(new AdministerResourceGrantRequest(Optional.empty(), GRANT)))
                .assertNext(response -> assertThat(response).isEqualTo(RESPONSE))
                .verifyComplete();
        assertThat(received).containsExactly(GRANT);
    }

    @Test
    void does_not_block_the_result_when_the_audit_repository_fails() {
        List<GrantResourceRequest> received = new ArrayList<>();
        AdministerResourceGrantUseCaseImpl useCase = new AdministerResourceGrantUseCaseImpl(
                allows(), grantResourceCapturing(received), TestAdministrationAuditRepositories.failing(),
                () -> FIXED_UUID, () -> FIXED_INSTANT);

        StepVerifier.create(useCase.execute(new AdministerResourceGrantRequest(Optional.of(ADMINISTRATION), GRANT)))
                .assertNext(response -> assertThat(response).isEqualTo(RESPONSE))
                .verifyComplete();
    }

    private static PrincipalMustBeApplicationAdministratorValidator allows() {
        return request -> Mono.empty();
    }

    private static PrincipalMustBeApplicationAdministratorValidator denies() {
        return request -> Mono.error(new NotAuthorizedToAdministerException(TENANT, APPLICATION));
    }

    private static GrantResourceToRoleUseCase grantResourceCapturing(List<GrantResourceRequest> received) {
        return input -> {
            received.add(input);
            return Mono.just(RESPONSE);
        };
    }
}
