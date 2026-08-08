package co.edu.uco.seguridad.pdp.recursos.application.exception;

import co.edu.uco.seguridad.crosscutting.messages.RecursosMessages;
import co.edu.uco.seguridad.pdp.commons.TenantId;
import co.edu.uco.seguridad.pdp.commons.exception.BusinessRuleViolationException;

/**
 * Generado por {@code ProtectedResourceMustBelongToApplicationTenantRule} y por nada más.
 *
 * <p>Protege contra un recurso adjunto a una aplicación propiedad de un inquilino diferente,
 * que en un punto de decisión de política significaría que un inquilino otorga acceso sobre datos de otro.</p>
 *
 * <p>Vive en {@code application} porque la regla que la lanza compara el comando contra la aplicación
 * ya registrada en otro módulo — una comparación entre dos agregados, no un invariante propio de un
 * único objeto de valor.</p>
 */
public final class ResourceTenantMismatchException extends BusinessRuleViolationException {

    public ResourceTenantMismatchException(TenantId requested, TenantId owning) {
        super("RESOURCE_TENANT_MISMATCH",
                RecursosMessages.resourceTenantMismatch(requested.value(), owning.value()));
    }
}
