package co.edu.uco.seguridad.pdp.assignments.application.primaryport.request;

import co.edu.uco.seguridad.pdp.assignments.domain.model.ProfileAssignmentId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;

import java.util.Objects;

/**
 * Entrada tipada de RevokeProfileAssignmentUseCase. El tenant ya llega resuelto del principal.
 */
public record RevokeProfileAssignmentRequest(ProfileAssignmentId profileAssignmentId, TenantId tenantId) {

    public RevokeProfileAssignmentRequest {
        Objects.requireNonNull(profileAssignmentId, RequiredArgumentMessages.PROFILE_ASSIGNMENT_ID);
        Objects.requireNonNull(tenantId, RequiredArgumentMessages.TENANT_ID);
    }
}
