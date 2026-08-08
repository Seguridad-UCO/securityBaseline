package co.edu.uco.seguridad.pdp.recursos.application.rule.impl;

import co.edu.uco.seguridad.pdp.commons.TenantId;
import co.edu.uco.seguridad.pdp.recursos.application.exception.ResourceTenantMismatchException;
import co.edu.uco.seguridad.pdp.recursos.application.rule.ProtectedResourceMustBelongToApplicationTenantRule;
import co.edu.uco.seguridad.pdp.recursos.application.rule.ProtectedResourceRegistration;

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
