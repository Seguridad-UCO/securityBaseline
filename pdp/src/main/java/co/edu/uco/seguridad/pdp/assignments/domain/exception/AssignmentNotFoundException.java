package co.edu.uco.seguridad.pdp.assignments.domain.exception;

import co.edu.uco.seguridad.pdp.assignments.domain.message.AssignmentsMessages;
import co.edu.uco.seguridad.pdp.assignments.domain.model.AssignmentId;
import co.edu.uco.seguridad.pdp.commons.exception.BusinessRuleViolationException;

/**
 * La asignación referenciada no existe, o no es de ese tenant.
 */
public final class AssignmentNotFoundException extends BusinessRuleViolationException {

    public AssignmentNotFoundException(AssignmentId assignmentId) {
        super("ASSIGNMENT_NOT_FOUND", AssignmentsMessages.assignmentNotFound(assignmentId.value().toString()));
    }
}
