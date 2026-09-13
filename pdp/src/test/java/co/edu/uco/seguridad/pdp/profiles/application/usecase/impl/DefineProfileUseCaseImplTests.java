package co.edu.uco.seguridad.pdp.profiles.application.usecase.impl;

import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.PageWindow;
import co.edu.uco.seguridad.pdp.commons.model.ProfileId;
import co.edu.uco.seguridad.pdp.commons.model.ResultPage;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.profiles.application.primaryport.request.DefineProfileRequest;
import co.edu.uco.seguridad.pdp.profiles.application.rule.validator.DefineProfileRulesValidator;
import co.edu.uco.seguridad.pdp.profiles.application.secondaryport.repository.ProfileRepository;
import co.edu.uco.seguridad.pdp.profiles.domain.Profile;
import co.edu.uco.seguridad.pdp.profiles.domain.ProfileCriteria;
import co.edu.uco.seguridad.pdp.profiles.domain.model.ProfileName;
import co.edu.uco.seguridad.pdp.roles.domain.model.RoleScope;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Con las reglas sustituidas por un dummy que siempre aprueba: aquí se prueba la construcción y la
 * persistencia, no las reglas — espejo de DefineRoleUseCaseImplTests. Cubre el alcance TENANT y
 * APPLICATION (criterio 3 del plan); el rechazo por nombre duplicado o aplicación inexistente es
 * indistinguible desde este caso de uso (lo decide el validador), igual que en roles.
 */
class DefineProfileUseCaseImplTests {

    private static final TenantId TENANT = new TenantId("universidad-uco");
    private static final Instant NOW = Instant.parse("2026-09-12T00:00:00Z");

    @Test
    void defines_a_tenant_scoped_profile_with_a_generated_id_and_the_current_time_and_no_roles() {
        List<Profile> saved = new ArrayList<>();
        UUID fixedId = UUID.randomUUID();
        DefineProfileUseCaseImpl useCase = new DefineProfileUseCaseImpl(
                dto -> Mono.empty(), repositoryCapturing(saved), () -> fixedId, () -> NOW);

        DefineProfileRequest request = new DefineProfileRequest(new ProfileName("Coordinador académico"),
                RoleScope.ofTenant(TENANT));

        StepVerifier.create(useCase.execute(request))
                .assertNext(response -> {
                    assertThat(response.id()).isEqualTo(new ProfileId(fixedId));
                    assertThat(response.scope()).isEqualTo(RoleScope.ofTenant(TENANT));
                    assertThat(response.roles()).isEmpty();
                    assertThat(response.registeredAt()).isEqualTo(NOW);
                })
                .verifyComplete();

        assertThat(saved).hasSize(1);
    }

    @Test
    void defines_an_application_scoped_profile() {
        List<Profile> saved = new ArrayList<>();
        UUID fixedId = UUID.randomUUID();
        ApplicationId application = new ApplicationId(UUID.randomUUID());
        DefineProfileUseCaseImpl useCase = new DefineProfileUseCaseImpl(
                dto -> Mono.empty(), repositoryCapturing(saved), () -> fixedId, () -> NOW);

        DefineProfileRequest request = new DefineProfileRequest(new ProfileName("Coordinador académico"),
                RoleScope.ofApplication(TENANT, application));

        StepVerifier.create(useCase.execute(request))
                .assertNext(response -> assertThat(response.scope()).isEqualTo(RoleScope.ofApplication(TENANT, application)))
                .verifyComplete();
    }

    @Test
    void never_saves_when_the_rules_validator_rejects_the_request() {
        RuntimeException rejection = new RuntimeException("nombre repetido");
        DefineProfileRulesValidator alwaysRejects = dto -> Mono.error(rejection);
        DefineProfileUseCaseImpl useCase = new DefineProfileUseCaseImpl(
                alwaysRejects, unreachableRepository(), UUID::randomUUID, () -> NOW);

        DefineProfileRequest request = new DefineProfileRequest(new ProfileName("Coordinador académico"),
                RoleScope.ofTenant(TENANT));

        StepVerifier.create(useCase.execute(request)).expectErrorMessage("nombre repetido").verify();
    }

    private static ProfileRepository repositoryCapturing(List<Profile> saved) {
        return new ProfileRepository() {
            @Override
            public Mono<Boolean> existsByNameInScope(ProfileName name, RoleScope scope) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Mono<Profile> findByIdForTenant(ProfileId profileId, TenantId tenantId) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Mono<ResultPage<Profile>> findBy(ProfileCriteria criteria, PageWindow window) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Mono<Profile> save(Profile profile) {
                saved.add(profile);
                return Mono.just(profile);
            }
        };
    }

    private static ProfileRepository unreachableRepository() {
        return new ProfileRepository() {
            @Override
            public Mono<Boolean> existsByNameInScope(ProfileName name, RoleScope scope) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Mono<Profile> findByIdForTenant(ProfileId profileId, TenantId tenantId) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Mono<ResultPage<Profile>> findBy(ProfileCriteria criteria, PageWindow window) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Mono<Profile> save(Profile profile) {
                throw new AssertionError("must not save when the rules reject the request");
            }
        };
    }
}
