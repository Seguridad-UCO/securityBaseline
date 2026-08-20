package co.edu.uco.seguridad.pdp.applications.application.exception;

import co.edu.uco.seguridad.pdp.applications.application.message.ApplicationsMessages;
import co.edu.uco.seguridad.pdp.commons.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.exception.BusinessRuleViolationException;

/** La aplicación referenciada no existe, o no pertenece al inquilino de quien la referencia. */
public final class ApplicationNotFoundException extends BusinessRuleViolationException {

    public ApplicationNotFoundException(ApplicationId applicationId) {
        super("APPLICATION_NOT_FOUND", ApplicationsMessages.applicationNotFound(applicationId.value().toString()));
    }
}
