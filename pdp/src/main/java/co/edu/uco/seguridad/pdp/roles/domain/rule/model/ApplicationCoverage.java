package co.edu.uco.seguridad.pdp.roles.domain.rule.model;

import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.roles.domain.model.RoleScope;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;

import java.util.Objects;

/**
 * Hecho ya resuelto para {@code RoleScopeMustCoverApplicationRule}: el alcance del rol y la
 * aplicación (y su tenant) que debe cubrir. Mismos campos que {@code ResourceCoverage} (HU-004) por
 * coincidencia de forma en la matemática de cobertura, no de significado — ver PLAN-HU-005 §3.
 */
public record ApplicationCoverage(RoleScope scope, ApplicationId applicationId, TenantId tenantId) {

    public ApplicationCoverage {
        Objects.requireNonNull(scope, RequiredArgumentMessages.ROLE_SCOPE);
        Objects.requireNonNull(applicationId, RequiredArgumentMessages.APPLICATION_ID);
        Objects.requireNonNull(tenantId, RequiredArgumentMessages.TENANT_ID);
    }
}
