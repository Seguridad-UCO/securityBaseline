package co.edu.uco.seguridad.pdp.profiles.domain;

import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;

import java.util.Objects;

/**
 * Specification de consulta del catálogo visible para un inquilino: sus perfiles más los globales.
 * Espejo de {@code RoleCriteria}.
 */
public record ProfileCriteria(TenantId tenantId, java.util.Optional<ApplicationId> applicationId) {

    public ProfileCriteria(TenantId tenantId) {
        this(tenantId, java.util.Optional.empty());
    }

    public ProfileCriteria {
        Objects.requireNonNull(tenantId, RequiredArgumentMessages.TENANT_ID);
        Objects.requireNonNull(applicationId, RequiredArgumentMessages.APPLICATION_ID);
    }

    public static ProfileCriteria ofTenant(TenantId tenantId) {
        return new ProfileCriteria(tenantId, java.util.Optional.empty());
    }

    /**
     * Consulta exclusivamente los perfiles con alcance de la aplicación indicada.
     */
    public static ProfileCriteria ofApplication(TenantId tenantId, ApplicationId applicationId) {
        return new ProfileCriteria(tenantId, java.util.Optional.of(applicationId));
    }

    public boolean matches(Profile profile) {
        Objects.requireNonNull(profile, RequiredArgumentMessages.PROFILE);
        if (applicationId.isPresent()) {
            return profile.scope().tenantId().filter(tenantId::equals).isPresent()
                    && profile.scope().applicationId().filter(applicationId.get()::equals).isPresent();
        }
        return profile.scope().isGlobal() || profile.scope().tenantId().map(tenantId::equals).orElse(false);
    }
}
