package co.edu.uco.seguridad.pdp.tenants.domain.rule.impl;

import co.edu.uco.seguridad.pdp.tenants.domain.rule.model.TenantCodeAvailability;
import co.edu.uco.seguridad.pdp.tenants.domain.exception.DuplicateTenantException;
import co.edu.uco.seguridad.pdp.tenants.domain.rule.TenantCodeMustBeUniqueRule;

public final class TenantCodeMustBeUniqueRuleImpl implements TenantCodeMustBeUniqueRule {

    @Override
    public void execute(TenantCodeAvailability availability) {
        if (availability.alreadyRegistered()) {
            throw new DuplicateTenantException(availability.tenantId());
        }
    }
}
