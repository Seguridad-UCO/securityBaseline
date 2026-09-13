package co.edu.uco.seguridad.pdp.assignments.domain.exception;

import co.edu.uco.seguridad.pdp.assignments.domain.message.AssignmentsMessages;
import co.edu.uco.seguridad.pdp.assignments.domain.model.ProfileAssignmentId;
import co.edu.uco.seguridad.pdp.commons.exception.BusinessRuleViolationException;

/** La asignación de perfil no existe, o no es de este inquilino. */
public final class ProfileAssignmentNotFoundException extends BusinessRuleViolationException {

    public ProfileAssignmentNotFoundException(ProfileAssignmentId profileAssignmentId) {
        super("PROFILE_ASSIGNMENT_NOT_FOUND",
                AssignmentsMessages.profileAssignmentNotFound(profileAssignmentId.value().toString()));
    }
}
