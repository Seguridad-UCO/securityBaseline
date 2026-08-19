package co.edu.uco.seguridad.pdp.tenants.application.exception;

import co.edu.uco.seguridad.crosscutting.messages.TenantsMessages;
import co.edu.uco.seguridad.pdp.commons.TenantId;
import co.edu.uco.seguridad.pdp.commons.exception.BusinessRuleViolationException;
import co.edu.uco.seguridad.pdp.tenants.domain.TenantStatus;

/** El inquilino existe pero su estado actual prohíbe la operación. */
public final class TenantNotActiveException extends BusinessRuleViolationException {

    public TenantNotActiveException(TenantId tenantId, TenantStatus status) {
        super("TENANT_NOT_ACTIVE", TenantsMessages.tenantNotActive(tenantId.value(), status.name()));
    }
}
