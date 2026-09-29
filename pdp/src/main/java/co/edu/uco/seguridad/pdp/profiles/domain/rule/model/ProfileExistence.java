package co.edu.uco.seguridad.pdp.profiles.domain.rule.model;

import co.edu.uco.seguridad.pdp.commons.model.ProfileId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;

import java.util.Objects;

/**
 * Hecho ya resuelto para ProfileMustExistForTenantRule: si el perfil existe para ese inquilino.
 */
public record ProfileExistence(ProfileId profileId, TenantId tenantId, boolean registered) {

    public ProfileExistence {
        Objects.requireNonNull(profileId, RequiredArgumentMessages.PROFILE_ID);
        Objects.requireNonNull(tenantId, RequiredArgumentMessages.TENANT_ID);
    }
}
