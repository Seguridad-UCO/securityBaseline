package co.edu.uco.seguridad.pdp.assignments.application.primaryport.request;

import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.ProfileId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.commons.model.UserId;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;

import java.util.Objects;

/** Entrada tipada de AssignProfileUseCase. El tenant ya llega resuelto del principal. */
public record AssignProfileRequest(TenantId tenantId, UserId userId, ApplicationId applicationId, ProfileId profileId) {

    public AssignProfileRequest {
        Objects.requireNonNull(tenantId, RequiredArgumentMessages.TENANT_ID);
        Objects.requireNonNull(userId, RequiredArgumentMessages.USER_ID);
        Objects.requireNonNull(applicationId, RequiredArgumentMessages.APPLICATION_ID);
        Objects.requireNonNull(profileId, RequiredArgumentMessages.PROFILE_ID);
    }
}
