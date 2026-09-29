package co.edu.uco.seguridad.pdp.identity.application.primaryport.request;

import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.commons.model.UserId;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;

import java.util.Objects;

/**
 * DTO de entrada del puerto primario: intención tipada de reasignar el tenant de un usuario.
 */
public record AssignTenantRequest(UserId userId, TenantId tenantId) {

    public AssignTenantRequest {
        Objects.requireNonNull(userId, RequiredArgumentMessages.USER_ID);
        Objects.requireNonNull(tenantId, RequiredArgumentMessages.TENANT_ID);
    }
}
