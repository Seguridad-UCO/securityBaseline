package co.edu.uco.seguridad.pdp.assignments.domain.rule.model;

import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.RoleId;
import co.edu.uco.seguridad.pdp.commons.model.UserId;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;

import java.util.Objects;

/**
 * Hecho ya resuelto para {@code AssignmentMustNotDuplicateActiveRule}: si ya hay una asignación activa para la tripleta.
 */
public record ActiveAssignmentAvailability(UserId userId, ApplicationId applicationId, RoleId roleId, boolean taken) {

    public ActiveAssignmentAvailability {
        Objects.requireNonNull(userId, RequiredArgumentMessages.USER_ID);
        Objects.requireNonNull(applicationId, RequiredArgumentMessages.APPLICATION_ID);
        Objects.requireNonNull(roleId, RequiredArgumentMessages.ROLE_ID);
    }
}
