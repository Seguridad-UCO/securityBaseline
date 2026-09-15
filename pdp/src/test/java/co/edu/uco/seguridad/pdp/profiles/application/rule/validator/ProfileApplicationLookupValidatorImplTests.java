package co.edu.uco.seguridad.pdp.profiles.application.rule.validator;

import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.PageWindow;
import co.edu.uco.seguridad.pdp.commons.model.ProfileId;
import co.edu.uco.seguridad.pdp.commons.model.ResultPage;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.profiles.application.primaryport.request.ProfileOwnershipQuery;
import co.edu.uco.seguridad.pdp.profiles.application.rule.validator.impl.ProfileApplicationLookupValidatorImpl;
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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** HU-019: resuelve a qué aplicación pertenece un perfil, para gatear AddRoleToProfile. */
class ProfileApplicationLookupValidatorImplTests {

    private static final TenantId TENANT = new TenantId("universidad-uco");
    private static final ProfileId PROFILE = new ProfileId(UUID.randomUUID());
    private static final ApplicationId APPLICATION = new ApplicationId(UUID.randomUUID());
    private static final Instant REGISTERED_AT = Instant.parse("2026-09-15T00:00:00Z");

    @Test
    void resolves_the_application_id_of_an_application_scoped_profile() {
        Profile profile = Profile.define(PROFILE, new ProfileName("Docentes de matematicas"),
                RoleScope.ofApplication(TENANT, APPLICATION), REGISTERED_AT);
        ProfileApplicationLookupValidatorImpl validator = new ProfileApplicationLookupValidatorImpl(
                repositoryFinding(profile), new ProfileMustExistForTenantRuleImpl());

        StepVerifier.create(validator.execute(new ProfileOwnershipQuery(TENANT, PROFILE)))
                .assertNext(result -> assertThat(result).contains(APPLICATION))
                .verifyComplete();
    }

    @Test
    void resolves_empty_for_a_tenant_scoped_profile() {
        Profile profile = Profile.define(PROFILE, new ProfileName("Coordinadores"), RoleScope.ofTenant(TENANT),
                REGISTERED_AT);
        ProfileApplicationLookupValidatorImpl validator = new ProfileApplicationLookupValidatorImpl(
                repositoryFinding(profile), new ProfileMustExistForTenantRuleImpl());

        StepVerifier.create(validator.execute(new ProfileOwnershipQuery(TENANT, PROFILE)))
                .assertNext(result -> assertThat(result).isEqualTo(Optional.<ApplicationId>empty()))
                .verifyComplete();
    }

    @Test
    void refuses_a_profile_that_does_not_exist_for_the_tenant() {
        ProfileApplicationLookupValidatorImpl validator = new ProfileApplicationLookupValidatorImpl(
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
