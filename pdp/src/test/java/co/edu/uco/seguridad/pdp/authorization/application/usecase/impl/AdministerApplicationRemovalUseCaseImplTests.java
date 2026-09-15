package co.edu.uco.seguridad.pdp.authorization.application.usecase.impl;

import co.edu.uco.seguridad.pdp.applications.application.usecase.RemoveApplicationUseCase;
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

/**
 * No decide nada de negocio propio: valida con el mecanismo de HU-009 y delega en
 * {@code RemoveApplicationUseCase}, que no cambia (PLAN-HU-015.md §0).
 */
class AdministerApplicationRemovalUseCaseImplTests {

    private static final TenantId TENANT = new TenantId("universidad-uco");
    private static final ApplicationId APPLICATION = new ApplicationId(UUID.randomUUID());
    private static final UserId USER = new UserId(UUID.randomUUID());
    private static final AdministrationRequest REQUEST =
            new AdministrationRequest(TENANT, APPLICATION, USER, "test-subject", Set.of());
    private static final UUID FIXED_UUID = UUID.randomUUID();
    private static final Instant FIXED_INSTANT = Instant.parse("2026-09-15T00:00:00Z");

    @Test
    void removes_the_application_when_the_principal_administers_it_and_audits_allowed() {
        List<ApplicationId> removed = new ArrayList<>();
        List<AdministrationEvent> audited = new ArrayList<>();
        AdministerApplicationRemovalUseCaseImpl useCase = new AdministerApplicationRemovalUseCaseImpl(
                allows(), removeApplicationCapturing(removed), TestAdministrationAuditRepositories.capturing(audited),
                () -> FIXED_UUID, () -> FIXED_INSTANT);

        StepVerifier.create(useCase.execute(REQUEST)).verifyComplete();

        assertThat(removed).containsExactly(APPLICATION);
        assertThat(audited).hasSize(1);
        assertThat(audited.getFirst().operation()).isEqualTo(AdministrationOperation.APPLICATION_REMOVED);
        assertThat(audited.getFirst().outcome()).isEqualTo(AdministrationOutcome.ALLOWED);
    }

    @Test
    void never_removes_the_application_when_the_principal_does_not_administer_it_and_audits_denied() {
        RemoveApplicationUseCase removeApplication = id -> { throw new AssertionError("must not reach removal"); };
        List<AdministrationEvent> audited = new ArrayList<>();
        AdministerApplicationRemovalUseCaseImpl useCase = new AdministerApplicationRemovalUseCaseImpl(denies(), removeApplication,
                TestAdministrationAuditRepositories.capturing(audited), () -> FIXED_UUID, () -> FIXED_INSTANT);

        StepVerifier.create(useCase.execute(REQUEST))
                .expectError(NotAuthorizedToAdministerException.class)
                .verify();

        assertThat(audited).hasSize(1);
        assertThat(audited.getFirst().outcome()).isEqualTo(AdministrationOutcome.DENIED);
    }

    @Test
    void does_not_block_the_result_when_the_audit_repository_fails() {
        List<ApplicationId> removed = new ArrayList<>();
        AdministerApplicationRemovalUseCaseImpl useCase = new AdministerApplicationRemovalUseCaseImpl(
                allows(), removeApplicationCapturing(removed), TestAdministrationAuditRepositories.failing(),
                () -> FIXED_UUID, () -> FIXED_INSTANT);

        StepVerifier.create(useCase.execute(REQUEST)).verifyComplete();

        assertThat(removed).containsExactly(APPLICATION);
    }

    private static PrincipalMustBeApplicationAdministratorValidator allows() {
        return request -> Mono.empty();
    }

    private static PrincipalMustBeApplicationAdministratorValidator denies() {
        return request -> Mono.error(new NotAuthorizedToAdministerException(TENANT, APPLICATION));
    }

    private static RemoveApplicationUseCase removeApplicationCapturing(List<ApplicationId> received) {
        return id -> {
            received.add(id);
            return Mono.empty();
        };
    }
}
