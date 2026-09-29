package co.edu.uco.seguridad.pdp.assignments.domain.exception;

import co.edu.uco.seguridad.pdp.assignments.domain.message.AssignmentsMessages;
import co.edu.uco.seguridad.pdp.commons.exception.BusinessRuleViolationException;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;

/**
 * No se puede revocar al único administrador activo de la aplicación (HU-020).
 */
public final class CannotRemoveLastAdministratorException extends BusinessRuleViolationException {

    public CannotRemoveLastAdministratorException(ApplicationId applicationId) {
        super("CANNOT_REMOVE_LAST_ADMINISTRATOR",
                AssignmentsMessages.cannotRemoveLastAdministrator(applicationId.value().toString()));
    }
}
