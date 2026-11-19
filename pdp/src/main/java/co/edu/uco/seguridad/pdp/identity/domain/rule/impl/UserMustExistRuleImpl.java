package co.edu.uco.seguridad.pdp.identity.domain.rule.impl;

import co.edu.uco.seguridad.pdp.identity.domain.exception.UserNotFoundException;
import co.edu.uco.seguridad.pdp.identity.domain.rule.UserMustExistRule;
import co.edu.uco.seguridad.pdp.identity.domain.rule.model.UserExistence;

public final class UserMustExistRuleImpl implements UserMustExistRule {

    @Override
    public void execute(UserExistence existence) {
        if (!existence.registered()) {
            throw new UserNotFoundException(existence.userId());
        }
    }
}
