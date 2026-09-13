package co.edu.uco.seguridad.pdp.profiles.domain;

import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;

import java.util.Objects;

/**
 * Specification de consulta del catálogo visible para un inquilino: sus perfiles más los globales.
 * Espejo de {@code RoleCriteria}.
 */
public record ProfileCriteria(TenantId tenantId) {

    public ProfileCriteria {
        Objects.requireNonNull(tenantId, RequiredArgumentMessages.TENANT_ID);
    }

    public static ProfileCriteria ofTenant(TenantId tenantId) {
        return new ProfileCriteria(tenantId);
    }

    public boolean matches(Profile profile) {
        Objects.requireNonNull(profile, RequiredArgumentMessages.PROFILE);
        if (profile.scope().isGlobal()) {
            return true;
        }
        return profile.scope().tenantId().map(tenantId::equals).orElse(false);
    }
}
