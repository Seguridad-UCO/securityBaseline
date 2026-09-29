package co.edu.uco.seguridad.pdp.resources.domain.exception;

import co.edu.uco.seguridad.pdp.commons.exception.ConflictBusinessRuleException;
import co.edu.uco.seguridad.pdp.resources.domain.message.ResourcesMessages;
import co.edu.uco.seguridad.pdp.resources.domain.model.HttpVerb;
import co.edu.uco.seguridad.pdp.resources.domain.model.ResourcePath;

/**
 * Generada por {@code ProtectedResourceMustBeUniqueRule}: el mismo método+ruta ya está registrado.
 */
public final class DuplicateProtectedResourceException extends ConflictBusinessRuleException {

    public DuplicateProtectedResourceException(ResourcePath path, HttpVerb method) {
        super("PROTECTED_RESOURCE_ALREADY_EXISTS",
                ResourcesMessages.duplicateProtectedResource(method.name(), path.value()));
    }
}
