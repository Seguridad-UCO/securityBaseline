package co.edu.uco.seguridad.pdp.assignments.domain.exception;

import co.edu.uco.seguridad.pdp.assignments.domain.message.AssignmentsMessages;
import co.edu.uco.seguridad.pdp.commons.exception.ConflictBusinessRuleException;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.RoleId;
import co.edu.uco.seguridad.pdp.commons.model.UserId;

/**
 * Ya existe una asignación activa para la misma tripleta (usuario, aplicación, rol).
 */
public final class DuplicateAssignmentException extends ConflictBusinessRuleException {

    public DuplicateAssignmentException(UserId userId, ApplicationId applicationId, RoleId roleId) {
        super("ASSIGNMENT_ALREADY_ACTIVE", AssignmentsMessages.assignmentAlreadyActive(
                userId.value().toString(), applicationId.value().toString(), roleId.value().toString()));
    }
}
