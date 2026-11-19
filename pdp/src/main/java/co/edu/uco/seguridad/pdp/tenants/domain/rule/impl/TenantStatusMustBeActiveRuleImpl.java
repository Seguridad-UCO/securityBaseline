package co.edu.uco.seguridad.pdp.tenants.domain.rule.impl;

import co.edu.uco.seguridad.pdp.tenants.domain.exception.TenantNotActiveException;
import co.edu.uco.seguridad.pdp.tenants.domain.rule.TenantStatusMustBeActiveRule;
import co.edu.uco.seguridad.pdp.tenants.domain.rule.model.TenantActivation;

public final class TenantStatusMustBeActiveRuleImpl implements TenantStatusMustBeActiveRule {

    @Override
    public void execute(TenantActivation activation) {
        if (!activation.status().allowsRegistration()) {
            throw new TenantNotActiveException(activation.tenantId(), activation.status());
        }
    }
}
