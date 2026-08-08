package co.edu.uco.seguridad.pdp.tenants.application.exception;

import co.edu.uco.seguridad.crosscutting.messages.TenantsMessages;
import co.edu.uco.seguridad.pdp.commons.TenantId;
import co.edu.uco.seguridad.pdp.commons.exception.BusinessRuleViolationException;

/**
 * El inquilino referenciado no existe. Distinto de {@link TenantNotActiveException}: un inquilino
 * desconocido es una solicitud incorrecta, uno suspendido es una decisión de política temporal.
 *
 * <p>Vive en {@code application} por la misma razón que {@link TenantNotActiveException}: la regla que la
 * lanza necesita el repositorio.</p>
 */
public final class TenantNotFoundException extends BusinessRuleViolationException {

    public TenantNotFoundException(TenantId tenantId) {
        super("TENANT_NOT_FOUND", TenantsMessages.tenantNotFound(tenantId.value()));
    }
}
