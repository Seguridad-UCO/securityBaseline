package co.edu.uco.seguridad.pdp.aplicaciones.application.exception;

import co.edu.uco.seguridad.crosscutting.messages.ApplicationsMessages;
import co.edu.uco.seguridad.pdp.commons.ApplicationName;
import co.edu.uco.seguridad.pdp.commons.exception.BusinessRuleViolationException;

/**
 * Generada por {@code ApplicationNameMustNotBeReservedRule}. Separada de
 * {@link DuplicateApplicationException}: un nombre reservado se rechaza para todo inquilino y para
 * siempre; un duplicado, solo mientras el otro registro exista.
 */
public final class ReservedApplicationNameException extends BusinessRuleViolationException {

    public ReservedApplicationNameException(ApplicationName name) {
        super("RESERVED_APPLICATION_NAME", ApplicationsMessages.reservedApplicationName(name.value()));
    }
}
