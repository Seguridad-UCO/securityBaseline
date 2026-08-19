package co.edu.uco.seguridad.pdp.applications.application.exception;

import co.edu.uco.seguridad.pdp.applications.application.message.ApplicationsMessages;
import co.edu.uco.seguridad.pdp.commons.ApplicationName;
import co.edu.uco.seguridad.pdp.commons.TenantId;
import co.edu.uco.seguridad.pdp.commons.exception.ConflictBusinessRuleException;

/**
 * Generado por {@code ApplicationNameMustBeUniqueForTenantRule} y por nada más.
 *
 * <p>Vive en {@code application} y no en {@code domain} porque la regla que la genera necesita el
 * repositorio para decidir — ya no es un invariante que {@code ApplicationName} pueda verificar por sí solo.</p>
 */
public final class DuplicateApplicationException extends ConflictBusinessRuleException {

    public DuplicateApplicationException(TenantId tenantId, ApplicationName name) {
        super("APPLICATION_ALREADY_EXISTS",
                ApplicationsMessages.duplicateApplication(tenantId.value(), name.value()));
    }
}
