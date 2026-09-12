package co.edu.uco.seguridad.pdp.assignments.domain.rule.model;

import co.edu.uco.seguridad.pdp.assignments.domain.model.AssignmentId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;

import java.util.Objects;

/** Hecho ya resuelto para {@code AssignmentMustExistForTenantRule}: si la asignación existe para ese tenant. */
public record AssignmentExistence(AssignmentId assignmentId, TenantId tenantId, boolean registered) {

    public AssignmentExistence {
        Objects.requireNonNull(assignmentId, RequiredArgumentMessages.ASSIGNMENT_ID);
        Objects.requireNonNull(tenantId, RequiredArgumentMessages.TENANT_ID);
    }
}
