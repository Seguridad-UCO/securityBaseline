package co.edu.uco.seguridad.pdp.profiles.application.primaryport.request;

import co.edu.uco.seguridad.pdp.commons.model.ProfileId;
import co.edu.uco.seguridad.pdp.commons.model.RoleId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;

import java.util.Objects;

/**
 * Solicitud para retirar un rol de un perfil existente.
 */
public record RemoveRoleFromProfileRequest(TenantId tenantId, ProfileId profileId, RoleId roleId) {
    public RemoveRoleFromProfileRequest {
        Objects.requireNonNull(tenantId, RequiredArgumentMessages.TENANT_ID);
        Objects.requireNonNull(profileId, RequiredArgumentMessages.PROFILE_ID);
        Objects.requireNonNull(roleId, RequiredArgumentMessages.ROLE_ID);
    }
}
