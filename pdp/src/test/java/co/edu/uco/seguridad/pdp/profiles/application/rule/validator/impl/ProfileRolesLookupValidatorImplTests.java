package co.edu.uco.seguridad.pdp.profiles.application.rule.validator.impl;

import co.edu.uco.seguridad.pdp.commons.model.PageWindow;
import co.edu.uco.seguridad.pdp.commons.model.ProfileId;
import co.edu.uco.seguridad.pdp.commons.model.ResultPage;
import co.edu.uco.seguridad.pdp.commons.model.RoleId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.profiles.application.primaryport.request.ProfileOwnershipQuery;
import co.edu.uco.seguridad.pdp.profiles.application.secondaryport.repository.ProfileRepository;
import co.edu.uco.seguridad.pdp.profiles.domain.Profile;
import co.edu.uco.seguridad.pdp.profiles.domain.ProfileCriteria;
import co.edu.uco.seguridad.pdp.profiles.domain.exception.ProfileNotFoundException;
import co.edu.uco.seguridad.pdp.profiles.domain.model.ProfileName;
import co.edu.uco.seguridad.pdp.profiles.domain.rule.impl.ProfileMustExistForTenantRuleImpl;
import co.edu.uco.seguridad.pdp.roles.domain.model.RoleScope;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A diferencia de RoleNamesLookupValidator (que omite en silencio), un perfil inexistente sí
 * rechaza — ver el Javadoc de ProfileRolesLookupValidator.
 */
class ProfileRolesLookupValidatorImplTests {

    private static final TenantId TENANT = new TenantId("universidad-uco");
    private static final ProfileId PROFILE = new ProfileId(UUID.randomUUID());
    private static final RoleId ROLE = new RoleId(UUID.randomUUID());

    @Test
    void returns_the_roles_of_a_profile_that_exists_for_the_tenant() {
        Profile profile = Profile.define(PROFILE, new ProfileName("Coordinador académico"), RoleScope.ofTenant(TENANT),
                Instant.parse("2026-09-12T00:00:00Z")).withRole(ROLE);
        ProfileRolesLookupValidatorImpl validator = new ProfileRolesLookupValidatorImpl(
                repositoryFinding(profile), new ProfileMustExistForTenantRuleImpl());

        StepVerifier.create(validator.execute(new ProfileOwnershipQuery(TENANT, PROFILE)))
                .assertNext(roles -> assertThat(roles).isEqualTo(Set.of(ROLE)))
                .verifyComplete();
    }

    @Test
    void rejects_a_profile_that_does_not_exist_for_the_tenant() {
        ProfileRolesLookupValidatorImpl validator = new ProfileRolesLookupValidatorImpl(
                repositoryFinding(null), new ProfileMustExistForTenantRuleImpl());

        StepVerifier.create(validator.execute(new ProfileOwnershipQuery(TENANT, PROFILE)))
                .expectError(ProfileNotFoundException.class)
                .verify();
    }

    private static ProfileRepository repositoryFinding(Profile found) {
        return new ProfileRepository() {
            @Override
            public Mono<Boolean> existsByNameInScope(ProfileName name, RoleScope scope) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Mono<Profile> findByIdForTenant(ProfileId profileId, TenantId tenantId) {
                return found == null ? Mono.empty() : Mono.just(found);
            }

            @Override
            public Mono<ResultPage<Profile>> findBy(ProfileCriteria criteria, PageWindow window) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Mono<Profile> save(Profile profile) {
                throw new UnsupportedOperationException();
            }
        };
    }
}
