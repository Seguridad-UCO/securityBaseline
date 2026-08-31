package co.edu.uco.seguridad.pdp.applications.application.primaryport.request;

import co.edu.uco.seguridad.pdp.commons.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.TenantId;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;

import java.util.Objects;

/**
 * Entrada de {@code ApplicationMustExistForTenantRule}: qué aplicación y de qué inquilino.
 * Los contratos base admiten un solo parámetro, así que la pareja viaja como un tipo propio.
 */
public record ApplicationOwnershipQuery(TenantId tenantId, ApplicationId applicationId) {

    public ApplicationOwnershipQuery {
        Objects.requireNonNull(tenantId, RequiredArgumentMessages.TENANT_ID);
        Objects.requireNonNull(applicationId, RequiredArgumentMessages.APPLICATION_ID);
    }
}
