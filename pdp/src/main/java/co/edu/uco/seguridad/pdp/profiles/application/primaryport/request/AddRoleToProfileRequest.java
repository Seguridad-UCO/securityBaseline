package co.edu.uco.seguridad.pdp.profiles.application.primaryport.request;

import co.edu.uco.seguridad.pdp.commons.model.ProfileId;
import co.edu.uco.seguridad.pdp.commons.model.RoleId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;

import java.util.Objects;

/**
 * Entrada tipada de AddRoleToProfileUseCase. El inquilino es el del principal: acota qué perfiles existen para él.
 */
public record AddRoleToProfileRequest(TenantId tenantId, ProfileId profileId, RoleId roleId) {

    public AddRoleToProfileRequest {
        Objects.requireNonNull(tenantId, RequiredArgumentMessages.TENANT_ID);
        Objects.requireNonNull(profileId, RequiredArgumentMessages.PROFILE_ID);
        Objects.requireNonNull(roleId, RequiredArgumentMessages.ROLE_ID);
    }
}
