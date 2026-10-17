package co.edu.uco.seguridad.pdp.applications.domain.exception;

import co.edu.uco.seguridad.pdp.applications.domain.message.ApplicationsMessages;
import co.edu.uco.seguridad.pdp.commons.exception.BusinessRuleViolationException;

/** La aplicación debe conservar al menos un ambiente disponible para el PEP. */
public final class ApplicationWithoutEnabledEnvironmentException extends BusinessRuleViolationException {

    public ApplicationWithoutEnabledEnvironmentException() {
        super("APPLICATION_WITHOUT_ENABLED_ENVIRONMENT", ApplicationsMessages.applicationWithoutEnabledEnvironment());
    }
}
