package co.edu.uco.seguridad.pdp.recursos.application.exception;

import co.edu.uco.seguridad.crosscutting.messages.RecursosMessages;
import co.edu.uco.seguridad.pdp.commons.exception.ConflictBusinessRuleException;
import co.edu.uco.seguridad.pdp.recursos.domain.ActionCode;
import co.edu.uco.seguridad.pdp.recursos.domain.ResourceCode;

/**
 * Generada por {@code ProtectedResourceMustBeUniqueRule}. Distinta de
 * {@code DuplicateApplicationException}: aquí la aplicación es legítima y solo se repite la concesión.
 */
public final class DuplicateProtectedResourceException extends ConflictBusinessRuleException {

    public DuplicateProtectedResourceException(ResourceCode resourceCode, ActionCode action) {
        super("PROTECTED_RESOURCE_ALREADY_EXISTS",
                RecursosMessages.duplicateProtectedResource(resourceCode.value(), action.value()));
    }
}
