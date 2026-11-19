package co.edu.uco.seguridad.pdp.assignments.domain;

import co.edu.uco.seguridad.pdp.commons.model.RoleId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;

import java.util.Objects;

/**
 * Specification de consulta del catálogo de asignaciones de un rol, aislado por tenant.
 */
public record AssignmentCriteria(RoleId roleId, TenantId tenantId) {

    public AssignmentCriteria {
        Objects.requireNonNull(roleId, RequiredArgumentMessages.ROLE_ID);
        Objects.requireNonNull(tenantId, RequiredArgumentMessages.TENANT_ID);
    }

    public static AssignmentCriteria of(RoleId roleId, TenantId tenantId) {
        return new AssignmentCriteria(roleId, tenantId);
    }

    public boolean matches(Assignment assignment) {
        Objects.requireNonNull(assignment, RequiredArgumentMessages.ASSIGNMENT);
        return assignment.roleId().equals(roleId) && assignment.tenantId().equals(tenantId);
    }
}
