package co.edu.uco.seguridad.pdp.resources.domain.exception;

import co.edu.uco.seguridad.pdp.commons.exception.BusinessRuleViolationException;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.resources.domain.message.ResourcesMessages;
import co.edu.uco.seguridad.pdp.resources.domain.model.HttpVerb;
import co.edu.uco.seguridad.pdp.resources.domain.model.ResourcePath;

/** El recurso protegido referenciado no existe bajo esa aplicacion. */
public final class ProtectedResourceNotFoundException extends BusinessRuleViolationException {

    public ProtectedResourceNotFoundException(ApplicationId applicationId, ResourcePath path, HttpVerb method) {
        super("PROTECTED_RESOURCE_NOT_FOUND",
                ResourcesMessages.protectedResourceNotFound(applicationId.value().toString(), path.value(), method.name()));
    }
}
