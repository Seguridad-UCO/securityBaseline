package co.edu.uco.seguridad.pdp.roles.domain.rule.model;

import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.roles.domain.model.RoleScope;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;

import java.util.Objects;

/**
 * Hecho ya resuelto para RoleScopeMustCoverResourceRule: el alcance del rol y a quién pertenece el recurso.
 */
public record ResourceCoverage(RoleScope scope, ApplicationId resourceApplicationId, TenantId resourceTenantId) {

    public ResourceCoverage {
        Objects.requireNonNull(scope, RequiredArgumentMessages.ROLE_SCOPE);
        Objects.requireNonNull(resourceApplicationId, RequiredArgumentMessages.APPLICATION_ID);
        Objects.requireNonNull(resourceTenantId, RequiredArgumentMessages.TENANT_ID);
    }
}
