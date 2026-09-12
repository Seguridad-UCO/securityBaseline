package co.edu.uco.seguridad.pdp.resources.domain.exception;

import co.edu.uco.seguridad.pdp.commons.exception.BusinessRuleViolationException;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.ResourceId;
import co.edu.uco.seguridad.pdp.resources.domain.message.ResourcesMessages;
import co.edu.uco.seguridad.pdp.resources.domain.model.HttpVerb;
import co.edu.uco.seguridad.pdp.resources.domain.model.ResourcePath;

/** El recurso protegido referenciado no existe bajo esa aplicacion, o no existe en absoluto (HU-004). */
public final class ProtectedResourceNotFoundException extends BusinessRuleViolationException {

    public ProtectedResourceNotFoundException(ApplicationId applicationId, ResourcePath path, HttpVerb method) {
        super("PROTECTED_RESOURCE_NOT_FOUND",
                ResourcesMessages.protectedResourceNotFound(applicationId.value().toString(), path.value(), method.name()));
    }

    /** HU-004: el validador de dueño de recurso solo tiene el id — no conoce aplicación, ruta ni método. */
    public ProtectedResourceNotFoundException(ResourceId resourceId) {
        super("PROTECTED_RESOURCE_NOT_FOUND", ResourcesMessages.protectedResourceNotFoundById(resourceId.value().toString()));
    }
}
