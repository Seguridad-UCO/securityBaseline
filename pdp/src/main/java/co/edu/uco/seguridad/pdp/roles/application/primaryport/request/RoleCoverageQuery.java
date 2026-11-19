package co.edu.uco.seguridad.pdp.roles.application.primaryport.request;

import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.RoleId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;

import java.util.Objects;

/**
 * Entrada de {@code RoleScopeMustCoverApplicationRule}: qué rol, de qué tenant, y qué aplicación
 * debe cubrir. Los contratos base admiten un solo parámetro, así que la tripleta viaja como un tipo
 * propio (mismo patrón que {@code ApplicationOwnershipQuery} de HU-003).
 */
public record RoleCoverageQuery(RoleId roleId, TenantId tenantId, ApplicationId applicationId) {

    public RoleCoverageQuery {
        Objects.requireNonNull(roleId, RequiredArgumentMessages.ROLE_ID);
        Objects.requireNonNull(tenantId, RequiredArgumentMessages.TENANT_ID);
        Objects.requireNonNull(applicationId, RequiredArgumentMessages.APPLICATION_ID);
    }
}
