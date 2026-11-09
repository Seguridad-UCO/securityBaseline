package co.edu.uco.seguridad.pdp.profiles.domain.rule.impl;

import co.edu.uco.seguridad.pdp.profiles.domain.exception.ProfileNotFoundException;
import co.edu.uco.seguridad.pdp.profiles.domain.rule.ProfileMustExistForTenantRule;
import co.edu.uco.seguridad.pdp.profiles.domain.rule.model.ProfileExistence;

public final class ProfileMustExistForTenantRuleImpl implements ProfileMustExistForTenantRule {

    @Override
    public void execute(ProfileExistence input) {
        if (!input.registered()) {
            throw new ProfileNotFoundException(input.profileId());
        }
    }
}
