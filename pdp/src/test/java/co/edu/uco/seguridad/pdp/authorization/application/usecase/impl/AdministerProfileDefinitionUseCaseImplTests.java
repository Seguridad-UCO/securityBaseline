package co.edu.uco.seguridad.pdp.authorization.application.usecase.impl;

import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AdministerProfileDefinitionRequest;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AdministrationRequest;
import co.edu.uco.seguridad.pdp.authorization.application.rule.validator.PrincipalMustBeApplicationAdministratorValidator;
import co.edu.uco.seguridad.pdp.authorization.domain.exception.NotAuthorizedToAdministerException;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.ProfileId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.commons.model.UserId;
import co.edu.uco.seguridad.pdp.profiles.application.primaryport.request.DefineProfileRequest;
import co.edu.uco.seguridad.pdp.profiles.application.primaryport.response.ProfileResponse;
import co.edu.uco.seguridad.pdp.profiles.application.usecase.DefineProfileUseCase;
import co.edu.uco.seguridad.pdp.profiles.domain.model.ProfileName;
import co.edu.uco.seguridad.pdp.roles.domain.model.RoleScope;
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
 * HU-019: gatea DefineProfileUseCase solo cuando hay administración que exigir (perfil
 * APPLICATION) — un perfil TENANT (administration vacío) delega directo, sin gate.
 */
class AdministerProfileDefinitionUseCaseImplTests {

    private static final TenantId TENANT = new TenantId("universidad-uco");
    private static final ApplicationId APPLICATION = new ApplicationId(UUID.randomUUID());
    private static final UserId USER = new UserId(UUID.randomUUID());
    private static final AdministrationRequest ADMINISTRATION =
            new AdministrationRequest(TENANT, APPLICATION, USER, "test-subject", Set.of());
    private static final DefineProfileRequest APPLICATION_SCOPED_PROFILE =
            new DefineProfileRequest(new ProfileName("Docentes de matematicas"), RoleScope.ofApplication(TENANT, APPLICATION));
    private static final DefineProfileRequest TENANT_SCOPED_PROFILE =
            new DefineProfileRequest(new ProfileName("Coordinadores"), RoleScope.ofTenant(TENANT));
    private static final ProfileResponse RESPONSE = new ProfileResponse(new ProfileId(UUID.randomUUID()),
            new ProfileName("Docentes de matematicas"), RoleScope.ofApplication(TENANT, APPLICATION), Set.of(),
            Instant.parse("2026-09-15T00:00:00Z"));

    @Test
    void defines_the_profile_when_the_principal_administers_the_application() {
        List<DefineProfileRequest> received = new ArrayList<>();
        AdministerProfileDefinitionUseCaseImpl useCase = new AdministerProfileDefinitionUseCaseImpl(
                allows(), defineProfileCapturing(received));

        StepVerifier.create(useCase.execute(new AdministerProfileDefinitionRequest(Optional.of(ADMINISTRATION), APPLICATION_SCOPED_PROFILE)))
                .assertNext(response -> assertThat(response).isEqualTo(RESPONSE))
                .verifyComplete();
        assertThat(received).containsExactly(APPLICATION_SCOPED_PROFILE);
    }

    @Test
    void never_defines_the_profile_when_the_principal_does_not_administer_the_application() {
        DefineProfileUseCase defineProfile = input -> {
            throw new AssertionError("must not reach DefineProfileUseCase");
        };
        AdministerProfileDefinitionUseCaseImpl useCase = new AdministerProfileDefinitionUseCaseImpl(denies(), defineProfile);

        StepVerifier.create(useCase.execute(new AdministerProfileDefinitionRequest(Optional.of(ADMINISTRATION), APPLICATION_SCOPED_PROFILE)))
                .expectError(NotAuthorizedToAdministerException.class)
                .verify();
    }

    @Test
    void defines_a_tenant_scoped_profile_without_gating_when_administration_is_empty() {
        List<DefineProfileRequest> received = new ArrayList<>();
        PrincipalMustBeApplicationAdministratorValidator neverCalled = request -> {
            throw new AssertionError("must not reach the administration gate");
        };
        AdministerProfileDefinitionUseCaseImpl useCase = new AdministerProfileDefinitionUseCaseImpl(
                neverCalled, defineProfileCapturing(received));

        StepVerifier.create(useCase.execute(new AdministerProfileDefinitionRequest(Optional.empty(), TENANT_SCOPED_PROFILE)))
                .assertNext(response -> assertThat(response).isEqualTo(RESPONSE))
                .verifyComplete();
        assertThat(received).containsExactly(TENANT_SCOPED_PROFILE);
    }

    private static PrincipalMustBeApplicationAdministratorValidator allows() {
        return request -> Mono.empty();
    }

    private static PrincipalMustBeApplicationAdministratorValidator denies() {
        return request -> Mono.error(new NotAuthorizedToAdministerException(TENANT, APPLICATION));
    }

    private static DefineProfileUseCase defineProfileCapturing(List<DefineProfileRequest> received) {
        return input -> {
            received.add(input);
            return Mono.just(RESPONSE);
        };
    }
}
