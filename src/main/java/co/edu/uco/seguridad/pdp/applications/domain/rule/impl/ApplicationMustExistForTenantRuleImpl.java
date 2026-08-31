package co.edu.uco.seguridad.pdp.applications.domain.rule.impl;

import co.edu.uco.seguridad.pdp.applications.domain.rule.model.ApplicationExistence;
import co.edu.uco.seguridad.pdp.applications.domain.exception.ApplicationNotFoundException;
import co.edu.uco.seguridad.pdp.applications.domain.rule.ApplicationMustExistForTenantRule;

public final class ApplicationMustExistForTenantRuleImpl implements ApplicationMustExistForTenantRule {

    @Override
    public void execute(ApplicationExistence existence) {
        if (!existence.registered()) {
            throw new ApplicationNotFoundException(existence.applicationId());
        }
    }
}
