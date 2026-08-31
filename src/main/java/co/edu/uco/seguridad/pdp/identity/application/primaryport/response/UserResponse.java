package co.edu.uco.seguridad.pdp.identity.application.primaryport.response;

import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.identity.domain.model.UserId;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;

import java.time.Instant;
import java.util.Objects;

/** DTO de salida del puerto primario: proyección de un usuario, con el proveedor de su login más reciente. */
public record UserResponse(UserId id, String email, String name, String provider, TenantId tenantId,
        Instant createdAt, Instant lastLoginAt) {

    public UserResponse {
        Objects.requireNonNull(id, RequiredArgumentMessages.USER_ID);
        Objects.requireNonNull(tenantId, RequiredArgumentMessages.TENANT_ID);
    }
}
