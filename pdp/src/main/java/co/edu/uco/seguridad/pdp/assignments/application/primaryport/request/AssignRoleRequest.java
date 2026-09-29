package co.edu.uco.seguridad.pdp.assignments.application.primaryport.request;

import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.RoleId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.commons.model.UserId;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;

import java.util.Objects;

/**
 * Entrada tipada de AssignRoleUseCase. El tenant ya llega resuelto del principal.
 */
public record AssignRoleRequest(TenantId tenantId, UserId userId, ApplicationId applicationId, RoleId roleId) {

    public AssignRoleRequest {
        Objects.requireNonNull(tenantId, RequiredArgumentMessages.TENANT_ID);
        Objects.requireNonNull(userId, RequiredArgumentMessages.USER_ID);
        Objects.requireNonNull(applicationId, RequiredArgumentMessages.APPLICATION_ID);
        Objects.requireNonNull(roleId, RequiredArgumentMessages.ROLE_ID);
    }
}
