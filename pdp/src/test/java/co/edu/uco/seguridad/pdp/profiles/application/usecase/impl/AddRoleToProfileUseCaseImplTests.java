package co.edu.uco.seguridad.pdp.profiles.application.usecase.impl;

import co.edu.uco.seguridad.pdp.commons.model.PageWindow;
import co.edu.uco.seguridad.pdp.commons.model.ProfileId;
import co.edu.uco.seguridad.pdp.commons.model.ResultPage;
import co.edu.uco.seguridad.pdp.commons.model.RoleId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.profiles.application.primaryport.request.AddRoleToProfileRequest;
import co.edu.uco.seguridad.pdp.profiles.application.rule.validator.AddRoleToProfileRulesValidator;
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
 * El validador ya devolvió el perfil correcto y validado (P2, P3) — este caso de uso solo lo
 * transforma (Profile.withRole) y lo guarda. Espejo de GrantResourceToRoleUseCaseImplTests.
 */
class AddRoleToProfileUseCaseImplTests {

    private static final TenantId TENANT = new TenantId("universidad-uco");
    private static final ProfileId PROFILE_ID = new ProfileId(UUID.randomUUID());
    private static final RoleId ROLE = new RoleId(UUID.randomUUID());

    @Test
    void saves_the_profile_with_the_role_added_and_returns_it() {
        Profile profile = Profile.define(PROFILE_ID, new ProfileName("Coordinador académico"),
                RoleScope.ofTenant(TENANT), Instant.parse("2026-09-12T00:00:00Z"));
        List<Profile> saved = new ArrayList<>();
        AddRoleToProfileUseCaseImpl useCase = new AddRoleToProfileUseCaseImpl(
                request -> Mono.just(profile), repositoryCapturing(saved));

        StepVerifier.create(useCase.execute(new AddRoleToProfileRequest(TENANT, PROFILE_ID, ROLE)))
                .assertNext(response -> assertThat(response.roles()).containsExactly(ROLE))
                .verifyComplete();

        assertThat(saved).hasSize(1);
        assertThat(saved.get(0).roles()).containsExactly(ROLE);
    }

    @Test
    void never_saves_when_the_rules_validator_rejects_the_request() {
        RuntimeException rejection = new RuntimeException("rol inexistente para el inquilino");
        AddRoleToProfileRulesValidator alwaysRejects = request -> Mono.error(rejection);
        AddRoleToProfileUseCaseImpl useCase = new AddRoleToProfileUseCaseImpl(alwaysRejects, unreachableRepository());

        StepVerifier.create(useCase.execute(new AddRoleToProfileRequest(TENANT, PROFILE_ID, ROLE)))
                .expectErrorMessage("rol inexistente para el inquilino")
                .verify();
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
