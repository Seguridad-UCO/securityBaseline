package co.edu.uco.seguridad.pdp.assignments.domain.rule.model;

import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.ProfileId;
import co.edu.uco.seguridad.pdp.commons.model.UserId;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;

import java.util.Objects;

/** Hecho ya resuelto para ProfileAssignmentMustNotDuplicateActiveRule. */
public record ActiveProfileAssignmentAvailability(UserId userId, ApplicationId applicationId, ProfileId profileId,
        boolean taken) {

    public ActiveProfileAssignmentAvailability {
        Objects.requireNonNull(userId, RequiredArgumentMessages.USER_ID);
        Objects.requireNonNull(applicationId, RequiredArgumentMessages.APPLICATION_ID);
        Objects.requireNonNull(profileId, RequiredArgumentMessages.PROFILE_ID);
    }
}
