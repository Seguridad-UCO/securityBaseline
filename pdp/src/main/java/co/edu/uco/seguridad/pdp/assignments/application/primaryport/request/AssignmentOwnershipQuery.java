package co.edu.uco.seguridad.pdp.assignments.application.primaryport.request;

import co.edu.uco.seguridad.pdp.assignments.domain.model.AssignmentId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;

import java.util.Objects;

/**
 * Entrada de {@code AssignmentApplicationLookupValidator} (HU-018): la asignación y el inquilino que pregunta por ella.
 */
public record AssignmentOwnershipQuery(AssignmentId assignmentId, TenantId tenantId) {

    public AssignmentOwnershipQuery {
        Objects.requireNonNull(assignmentId, RequiredArgumentMessages.ASSIGNMENT_ID);
        Objects.requireNonNull(tenantId, RequiredArgumentMessages.TENANT_ID);
    }
}
