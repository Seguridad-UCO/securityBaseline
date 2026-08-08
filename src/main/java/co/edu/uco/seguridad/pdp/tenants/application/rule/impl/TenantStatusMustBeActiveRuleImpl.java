package co.edu.uco.seguridad.pdp.tenants.application.rule.impl;

import co.edu.uco.seguridad.pdp.tenants.application.exception.TenantNotActiveException;
import co.edu.uco.seguridad.pdp.tenants.application.rule.TenantStatusMustBeActiveRule;
import co.edu.uco.seguridad.pdp.tenants.domain.Tenant;

public final class TenantStatusMustBeActiveRuleImpl implements TenantStatusMustBeActiveRule {

    @Override
    public void execute(Tenant tenant) {
        if (!tenant.isActive()) {
            throw new TenantNotActiveException(tenant.id(), tenant.status());
        }
    }
}
