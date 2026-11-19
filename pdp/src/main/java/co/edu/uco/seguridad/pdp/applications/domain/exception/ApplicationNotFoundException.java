package co.edu.uco.seguridad.pdp.applications.domain.exception;

import co.edu.uco.seguridad.pdp.applications.domain.message.ApplicationsMessages;
import co.edu.uco.seguridad.pdp.commons.exception.BusinessRuleViolationException;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationName;

/**
 * La aplicación referenciada no existe, o no pertenece al inquilino de quien la referencia.
 */
public final class ApplicationNotFoundException extends BusinessRuleViolationException {

    public ApplicationNotFoundException(ApplicationId applicationId) {
        super("APPLICATION_NOT_FOUND", ApplicationsMessages.applicationNotFound(applicationId.value().toString()));
    }

    public ApplicationNotFoundException(ApplicationName applicationName) {
        super("APPLICATION_NOT_FOUND", ApplicationsMessages.applicationNotFoundByName(applicationName.value()));
    }
}
