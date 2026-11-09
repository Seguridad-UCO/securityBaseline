package co.edu.uco.seguridad.pdp.profiles.domain.rule.impl;

import co.edu.uco.seguridad.pdp.commons.model.ProfileId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.profiles.domain.exception.ProfileNotFoundException;
import co.edu.uco.seguridad.pdp.profiles.domain.rule.model.ProfileExistence;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProfileMustExistForTenantRuleImplTests {

    private static final ProfileId PROFILE = new ProfileId(UUID.randomUUID());
    private static final TenantId TENANT = new TenantId("universidad-uco");

    @Test
    void rejects_a_profile_not_registered_for_that_tenant() {
        ProfileMustExistForTenantRuleImpl rule = new ProfileMustExistForTenantRuleImpl();

        assertThatThrownBy(() -> rule.execute(new ProfileExistence(PROFILE, TENANT, false)))
                .isInstanceOf(ProfileNotFoundException.class);
    }

    @Test
    void accepts_a_profile_registered_for_that_tenant() {
        ProfileMustExistForTenantRuleImpl rule = new ProfileMustExistForTenantRuleImpl();

        assertThatCode(() -> rule.execute(new ProfileExistence(PROFILE, TENANT, true))).doesNotThrowAnyException();
    }
}
