package co.edu.uco.seguridad.pdp.tenants.application.exception;

import co.edu.uco.seguridad.pdp.commons.TenantId;
import co.edu.uco.seguridad.pdp.commons.exception.ConflictBusinessRuleException;
import co.edu.uco.seguridad.pdp.tenants.application.message.TenantsMessages;

/** Generado por {@code TenantCodeMustBeUniqueRule} y por nada más. */
public final class DuplicateTenantException extends ConflictBusinessRuleException {

    public DuplicateTenantException(TenantId tenantId) {
        super("TENANT_ALREADY_EXISTS", TenantsMessages.duplicateTenant(tenantId.value()));
    }
}
