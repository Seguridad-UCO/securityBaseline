package co.edu.uco.seguridad.pdp.assignments.domain.exception;

import co.edu.uco.seguridad.pdp.assignments.domain.message.AssignmentsMessages;
import co.edu.uco.seguridad.pdp.commons.exception.ConflictBusinessRuleException;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.ProfileId;
import co.edu.uco.seguridad.pdp.commons.model.UserId;

/**
 * Ya hay una asignación activa de ese perfil para (usuario, aplicación).
 */
public final class DuplicateProfileAssignmentException extends ConflictBusinessRuleException {

    public DuplicateProfileAssignmentException(UserId userId, ApplicationId applicationId, ProfileId profileId) {
        super("PROFILE_ASSIGNMENT_ALREADY_ACTIVE", AssignmentsMessages.profileAssignmentAlreadyActive(
                userId.value().toString(), applicationId.value().toString(), profileId.value().toString()));
    }
}
