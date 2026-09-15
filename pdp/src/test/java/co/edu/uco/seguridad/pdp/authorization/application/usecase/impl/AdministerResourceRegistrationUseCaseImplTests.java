package co.edu.uco.seguridad.pdp.authorization.application.usecase.impl;

import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AdministerResourceRegistrationRequest;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AdministrationRequest;
import co.edu.uco.seguridad.pdp.authorization.application.rule.validator.PrincipalMustBeApplicationAdministratorValidator;
import co.edu.uco.seguridad.pdp.authorization.domain.exception.NotAuthorizedToAdministerException;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.ResourceId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.commons.model.UserId;
import co.edu.uco.seguridad.pdp.resources.application.primaryport.request.RegisterProtectedResourceRequest;
import co.edu.uco.seguridad.pdp.resources.application.primaryport.response.RegisteredProtectedResourceResponse;
import co.edu.uco.seguridad.pdp.resources.application.usecase.RegisterProtectedResourceUseCase;
import co.edu.uco.seguridad.pdp.resources.domain.model.HttpVerb;
import co.edu.uco.seguridad.pdp.resources.domain.model.ResourcePath;
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
 * HU-017: a diferencia de HU-016, el gate siempre se evalúa — sin {@code Optional}, un recurso
 * protegido siempre pertenece a una aplicación.
 */
class AdministerResourceRegistrationUseCaseImplTests {

    private static final TenantId TENANT = new TenantId("universidad-uco");
    private static final ApplicationId APPLICATION = new ApplicationId(UUID.randomUUID());
    private static final UserId USER = new UserId(UUID.randomUUID());
    private static final AdministrationRequest ADMINISTRATION =
            new AdministrationRequest(TENANT, APPLICATION, USER, "test-subject", Set.of());
    private static final RegisterProtectedResourceRequest RESOURCE = new RegisterProtectedResourceRequest(
            TENANT, APPLICATION, new ResourcePath("/estudiantes"), HttpVerb.GET);
    private static final RegisteredProtectedResourceResponse RESPONSE = new RegisteredProtectedResourceResponse(
            new ResourceId(UUID.randomUUID()), APPLICATION, TENANT, new ResourcePath("/estudiantes"), HttpVerb.GET,
            Instant.parse("2026-09-14T00:00:00Z"));
    private static final UUID FIXED_UUID = UUID.randomUUID();
    private static final Instant FIXED_INSTANT = Instant.parse("2026-09-15T00:00:00Z");

    @Test
    void registers_the_resource_when_the_principal_administers_the_application_and_audits_allowed() {
        List<RegisterProtectedResourceRequest> received = new ArrayList<>();
        List<AdministrationEvent> audited = new ArrayList<>();
        AdministerResourceRegistrationUseCaseImpl useCase = new AdministerResourceRegistrationUseCaseImpl(
                allows(), registerResourceCapturing(received), TestAdministrationAuditRepositories.capturing(audited),
                () -> FIXED_UUID, () -> FIXED_INSTANT);

        StepVerifier.create(useCase.execute(new AdministerResourceRegistrationRequest(ADMINISTRATION, RESOURCE)))
                .assertNext(response -> assertThat(response).isEqualTo(RESPONSE))
                .verifyComplete();
        assertThat(received).containsExactly(RESOURCE);
        assertThat(audited).hasSize(1);
        assertThat(audited.getFirst().operation()).isEqualTo(AdministrationOperation.RESOURCE_REGISTERED);
        assertThat(audited.getFirst().outcome()).isEqualTo(AdministrationOutcome.ALLOWED);
    }

    @Test
    void never_registers_the_resource_when_the_principal_does_not_administer_the_application_and_audits_denied() {
        RegisterProtectedResourceUseCase registerResource = input -> {
            throw new AssertionError("must not reach RegisterProtectedResourceUseCase");
        };
        List<AdministrationEvent> audited = new ArrayList<>();
        AdministerResourceRegistrationUseCaseImpl useCase = new AdministerResourceRegistrationUseCaseImpl(
                denies(), registerResource, TestAdministrationAuditRepositories.capturing(audited), () -> FIXED_UUID,
                () -> FIXED_INSTANT);

        StepVerifier.create(useCase.execute(new AdministerResourceRegistrationRequest(ADMINISTRATION, RESOURCE)))
                .expectError(NotAuthorizedToAdministerException.class)
                .verify();
        assertThat(audited).hasSize(1);
        assertThat(audited.getFirst().outcome()).isEqualTo(AdministrationOutcome.DENIED);
    }

    @Test
    void does_not_block_the_result_when_the_audit_repository_fails() {
        List<RegisterProtectedResourceRequest> received = new ArrayList<>();
        AdministerResourceRegistrationUseCaseImpl useCase = new AdministerResourceRegistrationUseCaseImpl(
                allows(), registerResourceCapturing(received), TestAdministrationAuditRepositories.failing(),
                () -> FIXED_UUID, () -> FIXED_INSTANT);

        StepVerifier.create(useCase.execute(new AdministerResourceRegistrationRequest(ADMINISTRATION, RESOURCE)))
                .assertNext(response -> assertThat(response).isEqualTo(RESPONSE))
                .verifyComplete();
    }

    private static PrincipalMustBeApplicationAdministratorValidator allows() {
        return request -> Mono.empty();
    }

    private static PrincipalMustBeApplicationAdministratorValidator denies() {
        return request -> Mono.error(new NotAuthorizedToAdministerException(TENANT, APPLICATION));
    }

    private static RegisterProtectedResourceUseCase registerResourceCapturing(List<RegisterProtectedResourceRequest> received) {
        return input -> {
            received.add(input);
            return Mono.just(RESPONSE);
        };
    }
}
