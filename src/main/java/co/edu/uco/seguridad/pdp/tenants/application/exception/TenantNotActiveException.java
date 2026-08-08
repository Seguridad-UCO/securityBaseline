package co.edu.uco.seguridad.pdp.tenants.application.exception;

import co.edu.uco.seguridad.crosscutting.messages.TenantsMessages;
import co.edu.uco.seguridad.pdp.commons.TenantId;
import co.edu.uco.seguridad.pdp.commons.exception.BusinessRuleViolationException;
import co.edu.uco.seguridad.pdp.tenants.TenantStatus;

/**
 * El inquilino existe pero su estado actual prohíbe la operación.
 *
 * <p>Vive en {@code application} y no en {@code domain} porque la regla que la genera
 * ({@code TenantMustBeActiveRule}) necesita el repositorio para decidir — ya no es un invariante que un
 * objeto de valor pueda verificar por sí solo.</p>
 */
public final class TenantNotActiveException extends BusinessRuleViolationException {

    public TenantNotActiveException(TenantId tenantId, TenantStatus status) {
        super("TENANT_NOT_ACTIVE", TenantsMessages.tenantNotActive(tenantId.value(), status.name()));
    }
}
