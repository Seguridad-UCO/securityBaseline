package co.edu.uco.seguridad.pdp.assignments.domain.exception;

import co.edu.uco.seguridad.pdp.assignments.domain.message.AssignmentsMessages;
import co.edu.uco.seguridad.pdp.commons.exception.InvalidValueException;

/** El fin de la vigencia no es posterior a su inicio. */
public final class InvalidValidityException extends InvalidValueException {

    public InvalidValidityException(String reason) {
        super("INVALID_VALIDITY", AssignmentsMessages.invalidValidity(reason));
    }
}
