package co.edu.uco.seguridad.pdp.tenants.domain.exception;

import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.commons.exception.BusinessRuleViolationException;
import co.edu.uco.seguridad.pdp.tenants.domain.model.TenantStatus;
import co.edu.uco.seguridad.pdp.tenants.domain.message.TenantsMessages;

/** El inquilino existe pero su estado actual prohíbe la operación. */
public final class TenantNotActiveException extends BusinessRuleViolationException {

    public TenantNotActiveException(TenantId tenantId, TenantStatus status) {
        super("TENANT_NOT_ACTIVE", TenantsMessages.tenantNotActive(tenantId.value(), status.name()));
    }
}
