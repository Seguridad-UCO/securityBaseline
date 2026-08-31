package co.edu.uco.seguridad.pdp.applications.domain.rule.model;

import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;

import java.util.Objects;

/**
 * Entrada ya resuelta de {@code ApplicationMustExistForTenantRule}: qué aplicación se preguntó, de
 * qué inquilino, y si el almacén la tenía bajo ese inquilino.
 */
public record ApplicationExistence(TenantId tenantId, ApplicationId applicationId, boolean registered) {

    public ApplicationExistence {
        Objects.requireNonNull(tenantId, RequiredArgumentMessages.TENANT_ID);
        Objects.requireNonNull(applicationId, RequiredArgumentMessages.APPLICATION_ID);
    }
}
