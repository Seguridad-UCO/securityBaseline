package co.edu.uco.seguridad.pdp.recursos.application.exception;

import co.edu.uco.seguridad.crosscutting.messages.RecursosMessages;
import co.edu.uco.seguridad.pdp.commons.TenantId;
import co.edu.uco.seguridad.pdp.commons.exception.BusinessRuleViolationException;

/**
 * Generada por {@code ProtectedResourceMustBelongToApplicationTenantRule}: protege contra un recurso
 * adjunto a una aplicación de otro inquilino.
 */
public final class ResourceTenantMismatchException extends BusinessRuleViolationException {

    public ResourceTenantMismatchException(TenantId requested, TenantId owning) {
        super("RESOURCE_TENANT_MISMATCH",
                RecursosMessages.resourceTenantMismatch(requested.value(), owning.value()));
    }
}
