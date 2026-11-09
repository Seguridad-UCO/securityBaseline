package co.edu.uco.seguridad.pdp.identity.domain.rule.impl;

import co.edu.uco.seguridad.pdp.identity.domain.exception.UserEmailAlreadyRegisteredException;
import co.edu.uco.seguridad.pdp.identity.domain.rule.UserEmailMustBeAvailableRule;
import co.edu.uco.seguridad.pdp.identity.domain.rule.model.UserEmailAvailability;

public final class UserEmailMustBeAvailableRuleImpl implements UserEmailMustBeAvailableRule {

    @Override
    public void execute(UserEmailAvailability input) {
        if (input.taken()) {
            throw new UserEmailAlreadyRegisteredException(input.email());
        }
    }
}
