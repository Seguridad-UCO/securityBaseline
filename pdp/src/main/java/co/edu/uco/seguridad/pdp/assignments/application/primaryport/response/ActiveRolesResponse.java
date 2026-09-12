package co.edu.uco.seguridad.pdp.assignments.application.primaryport.response;

import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.RoleId;
import co.edu.uco.seguridad.pdp.commons.model.UserId;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;

import java.util.Objects;
import java.util.Set;

/** El contexto resuelto que HU-006 enviará a OPA: qué roles tiene vigentes un sujeto en una aplicación. */
public record ActiveRolesResponse(UserId userId, ApplicationId applicationId, Set<RoleId> roleIds) {

    public ActiveRolesResponse {
        Objects.requireNonNull(userId, RequiredArgumentMessages.USER_ID);
        Objects.requireNonNull(applicationId, RequiredArgumentMessages.APPLICATION_ID);
        roleIds = Set.copyOf(Objects.requireNonNull(roleIds, RequiredArgumentMessages.ACTIVE_ROLE_IDS));
    }
}
