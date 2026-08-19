package co.edu.uco.seguridad.pdp.resources.application.exception;

import co.edu.uco.seguridad.pdp.commons.exception.ConflictBusinessRuleException;
import co.edu.uco.seguridad.pdp.resources.application.message.ResourcesMessages;
import co.edu.uco.seguridad.pdp.resources.domain.ActionCode;
import co.edu.uco.seguridad.pdp.resources.domain.ResourceCode;

/**
 * Generada por {@code ProtectedResourceMustBeUniqueRule}. Distinta de
 * {@code DuplicateApplicationException}: aquí la aplicación es legítima y solo se repite la concesión.
 */
public final class DuplicateProtectedResourceException extends ConflictBusinessRuleException {

    public DuplicateProtectedResourceException(ResourceCode resourceCode, ActionCode action) {
        super("PROTECTED_RESOURCE_ALREADY_EXISTS",
                ResourcesMessages.duplicateProtectedResource(resourceCode.value(), action.value()));
    }
}
