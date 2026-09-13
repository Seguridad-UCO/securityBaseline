package co.edu.uco.seguridad.pdp.applications.domain.rule.impl;

import co.edu.uco.seguridad.pdp.applications.domain.exception.InvalidApplicationCredentialException;
import co.edu.uco.seguridad.pdp.applications.domain.rule.ApplicationCredentialMustBeValidRule;
import co.edu.uco.seguridad.pdp.applications.domain.rule.model.ApplicationCredentialValidity;

public final class ApplicationCredentialMustBeValidRuleImpl implements ApplicationCredentialMustBeValidRule {

    @Override
    public void execute(ApplicationCredentialValidity input) {
        if (!input.valid()) {
            throw new InvalidApplicationCredentialException();
        }
    }
}
