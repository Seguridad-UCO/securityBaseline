package co.edu.uco.seguridad.pdp.resources.application.exception;

import co.edu.uco.seguridad.pdp.commons.TenantId;
import co.edu.uco.seguridad.pdp.commons.exception.BusinessRuleViolationException;
import co.edu.uco.seguridad.pdp.resources.application.message.ResourcesMessages;

/**
 * Generada por {@code ProtectedResourceMustBelongToApplicationTenantRule}: protege contra un recurso
 * adjunto a una aplicación de otro inquilino.
 */
public final class ResourceTenantMismatchException extends BusinessRuleViolationException {

    public ResourceTenantMismatchException(TenantId requested, TenantId owning) {
        super("RESOURCE_TENANT_MISMATCH",
                ResourcesMessages.resourceTenantMismatch(requested.value(), owning.value()));
    }
}
