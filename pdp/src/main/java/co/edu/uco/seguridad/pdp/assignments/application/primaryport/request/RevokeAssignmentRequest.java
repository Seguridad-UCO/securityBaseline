package co.edu.uco.seguridad.pdp.assignments.application.primaryport.request;

import co.edu.uco.seguridad.pdp.assignments.domain.model.AssignmentId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;

import java.util.Objects;

/**
 * Entrada tipada de RevokeAssignmentUseCase. El tenant ya llega resuelto del principal.
 */
public record RevokeAssignmentRequest(AssignmentId assignmentId, TenantId tenantId) {

    public RevokeAssignmentRequest {
        Objects.requireNonNull(assignmentId, RequiredArgumentMessages.ASSIGNMENT_ID);
        Objects.requireNonNull(tenantId, RequiredArgumentMessages.TENANT_ID);
    }
}
