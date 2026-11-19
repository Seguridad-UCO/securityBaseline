package co.edu.uco.seguridad.pdp.profiles.domain.rule.impl;

import co.edu.uco.seguridad.pdp.profiles.domain.exception.DuplicateProfileNameException;
import co.edu.uco.seguridad.pdp.profiles.domain.rule.ProfileNameMustBeUniqueInScopeRule;
import co.edu.uco.seguridad.pdp.profiles.domain.rule.model.ProfileNameAvailability;

public final class ProfileNameMustBeUniqueInScopeRuleImpl implements ProfileNameMustBeUniqueInScopeRule {

    @Override
    public void execute(ProfileNameAvailability input) {
        if (input.taken()) {
            throw new DuplicateProfileNameException(input.name(), input.scope());
        }
    }
}
