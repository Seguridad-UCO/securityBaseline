package co.edu.uco.seguridad.pdp.applications.domain.exception;

import co.edu.uco.seguridad.pdp.applications.domain.message.ApplicationsMessages;
import co.edu.uco.seguridad.pdp.commons.exception.BusinessRuleViolationException;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationName;

/**
 * Un nombre sin tenant no identifica de forma segura una aplicación del catálogo.
 */
public final class AmbiguousApplicationNameException extends BusinessRuleViolationException {

    public AmbiguousApplicationNameException(ApplicationName applicationName) {
        super("AMBIGUOUS_APPLICATION_NAME", ApplicationsMessages.ambiguousApplicationName(applicationName.value()));
    }
}
