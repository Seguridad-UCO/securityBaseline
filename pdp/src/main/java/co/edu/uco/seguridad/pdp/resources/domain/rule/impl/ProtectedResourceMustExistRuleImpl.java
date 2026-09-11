package co.edu.uco.seguridad.pdp.resources.domain.rule.impl;

import co.edu.uco.seguridad.pdp.resources.domain.exception.ProtectedResourceNotFoundException;
import co.edu.uco.seguridad.pdp.resources.domain.rule.ProtectedResourceMustExistRule;
import co.edu.uco.seguridad.pdp.resources.domain.rule.model.ProtectedResourceExistence;

public final class ProtectedResourceMustExistRuleImpl implements ProtectedResourceMustExistRule {

    @Override
    public void execute(ProtectedResourceExistence existence) {
        if (!existence.registered()) {
            throw new ProtectedResourceNotFoundException(existence.applicationId(), existence.path(), existence.method());
        }
    }
}
