package co.edu.uco.seguridad.pdp.applications.domain.exception;

import co.edu.uco.seguridad.pdp.applications.domain.message.ApplicationsMessages;
import co.edu.uco.seguridad.pdp.commons.exception.BusinessRuleViolationException;

/**
 * La credencial de una aplicación no es válida — la aplicación no existe o el secreto no coincide
 * con su hash. Nunca distingue cuál de las dos, a propósito (HU-013).
 */
public final class InvalidApplicationCredentialException extends BusinessRuleViolationException {

    public InvalidApplicationCredentialException() {
        super("INVALID_APPLICATION_CREDENTIAL", ApplicationsMessages.invalidApplicationCredential());
    }
}
