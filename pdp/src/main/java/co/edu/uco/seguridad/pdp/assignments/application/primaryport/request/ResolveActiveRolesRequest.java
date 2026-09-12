package co.edu.uco.seguridad.pdp.assignments.application.primaryport.request;

import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.UserId;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;

import java.util.Objects;

/** Entrada tipada de ResolveActiveRolesUseCase: el contexto que HU-006 enviará a OPA. */
public record ResolveActiveRolesRequest(UserId userId, ApplicationId applicationId) {

    public ResolveActiveRolesRequest {
        Objects.requireNonNull(userId, RequiredArgumentMessages.USER_ID);
        Objects.requireNonNull(applicationId, RequiredArgumentMessages.APPLICATION_ID);
    }
}
