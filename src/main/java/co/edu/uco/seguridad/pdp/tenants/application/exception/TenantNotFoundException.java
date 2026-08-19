package co.edu.uco.seguridad.pdp.tenants.application.exception;

import co.edu.uco.seguridad.pdp.commons.TenantId;
import co.edu.uco.seguridad.pdp.commons.exception.BusinessRuleViolationException;
import co.edu.uco.seguridad.pdp.tenants.application.message.TenantsMessages;

/**
 * El inquilino referenciado no existe. Distinto de {@link TenantNotActiveException}: desconocido es
 * una solicitud incorrecta, suspendido es una decisión de política temporal.
 */
public final class TenantNotFoundException extends BusinessRuleViolationException {

    public TenantNotFoundException(TenantId tenantId) {
        super("TENANT_NOT_FOUND", TenantsMessages.tenantNotFound(tenantId.value()));
    }
}
