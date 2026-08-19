package co.edu.uco.seguridad.pdp.resources.application.rule.impl;

import co.edu.uco.seguridad.pdp.commons.TenantId;
import co.edu.uco.seguridad.pdp.resources.application.exception.ResourceTenantMismatchException;
import co.edu.uco.seguridad.pdp.resources.application.model.ProtectedResourceRegistration;
import co.edu.uco.seguridad.pdp.resources.application.rule.ProtectedResourceMustBelongToApplicationTenantRule;

public final class ProtectedResourceMustBelongToApplicationTenantRuleImpl
        implements ProtectedResourceMustBelongToApplicationTenantRule {

    @Override
    public void execute(ProtectedResourceRegistration registration) {
        TenantId requested = registration.dto().tenantId();
        TenantId owning = registration.application().tenantId();
        if (!requested.equals(owning)) {
            throw new ResourceTenantMismatchException(requested, owning);
        }
    }
}
