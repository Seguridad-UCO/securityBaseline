package co.edu.uco.seguridad.pdp.applications.domain.rule.impl;

import co.edu.uco.seguridad.pdp.applications.domain.rule.model.ApplicationNameAvailability;
import co.edu.uco.seguridad.pdp.applications.domain.exception.DuplicateApplicationException;
import co.edu.uco.seguridad.pdp.applications.domain.rule.ApplicationNameMustBeUniqueForTenantRule;

public final class ApplicationNameMustBeUniqueForTenantRuleImpl implements ApplicationNameMustBeUniqueForTenantRule {

    @Override
    public void execute(ApplicationNameAvailability availability) {
        if (availability.alreadyRegistered()) {
            throw new DuplicateApplicationException(availability.tenantId(), availability.name());
        }
    }
}
