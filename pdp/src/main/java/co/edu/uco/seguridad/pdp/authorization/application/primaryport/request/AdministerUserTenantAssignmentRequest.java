package co.edu.uco.seguridad.pdp.authorization.application.primaryport.request;

import co.edu.uco.seguridad.pdp.identity.application.primaryport.request.AssignTenantRequest;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;

import java.util.Objects;

/**
 * Reasignación de inquilino pedida por un actor de inquilino (SU-01): quién la pide y qué pide.
 */
public record AdministerUserTenantAssignmentRequest(TenantAdministrationRequest actor, AssignTenantRequest assignment) {

    public AdministerUserTenantAssignmentRequest {
        Objects.requireNonNull(actor, RequiredArgumentMessages.SUBJECT);
        Objects.requireNonNull(assignment, RequiredArgumentMessages.USER_TENANT_ASSIGNMENT);
    }
}
