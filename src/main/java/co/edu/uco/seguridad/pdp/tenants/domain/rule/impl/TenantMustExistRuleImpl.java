package co.edu.uco.seguridad.pdp.tenants.domain.rule.impl;

import co.edu.uco.seguridad.pdp.tenants.domain.rule.model.TenantExistence;
import co.edu.uco.seguridad.pdp.tenants.domain.exception.TenantNotFoundException;
import co.edu.uco.seguridad.pdp.tenants.domain.rule.TenantMustExistRule;

public final class TenantMustExistRuleImpl implements TenantMustExistRule {

    @Override
    public void execute(TenantExistence existence) {
        if (!existence.registered()) {
            throw new TenantNotFoundException(existence.tenantId());
        }
    }
}
