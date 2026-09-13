package co.edu.uco.seguridad.pdp.profiles.domain;

import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.ProfileId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.profiles.domain.model.ProfileName;
import co.edu.uco.seguridad.pdp.roles.domain.model.RoleScope;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ProfileCriteriaTests {

    private static final TenantId UCO = new TenantId("universidad-uco");
    private static final TenantId OTRO = new TenantId("otra-universidad");
    private static final Instant REGISTERED_AT = Instant.parse("2026-09-12T00:00:00Z");

    @Test
    void matches_a_profile_of_the_same_tenant() {
        ProfileCriteria criteria = ProfileCriteria.ofTenant(UCO);

        assertThat(criteria.matches(profile(RoleScope.ofTenant(UCO)))).isTrue();
    }

    @Test
    void matches_a_global_profile_regardless_of_tenant() {
        ProfileCriteria criteria = ProfileCriteria.ofTenant(UCO);

        assertThat(criteria.matches(profile(RoleScope.global()))).isTrue();
    }

    @Test
    void rejects_a_profile_of_another_tenant() {
        ProfileCriteria criteria = ProfileCriteria.ofTenant(UCO);

        assertThat(criteria.matches(profile(RoleScope.ofTenant(OTRO)))).isFalse();
    }

    @Test
    void matches_an_application_profile_whose_tenant_is_the_same() {
        ProfileCriteria criteria = ProfileCriteria.ofTenant(UCO);
        RoleScope scope = RoleScope.ofApplication(UCO, new ApplicationId(UUID.randomUUID()));

        assertThat(criteria.matches(profile(scope))).isTrue();
    }

    private static Profile profile(RoleScope scope) {
        return Profile.define(new ProfileId(UUID.randomUUID()), new ProfileName("Coordinador académico"), scope,
                REGISTERED_AT);
    }
}
