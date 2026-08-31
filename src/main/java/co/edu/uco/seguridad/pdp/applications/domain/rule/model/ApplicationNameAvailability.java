package co.edu.uco.seguridad.pdp.applications.domain.rule.model;

import co.edu.uco.seguridad.pdp.commons.model.ApplicationName;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;

import java.util.Objects;

/**
 * Entrada ya resuelta de {@code ApplicationNameMustBeUniqueForTenantRule}: el par inquilino+nombre
 * y si ya estaba tomado. La unicidad es sobre el par, no sobre el nombre solo.
 */
public record ApplicationNameAvailability(TenantId tenantId, ApplicationName name, boolean alreadyRegistered) {

    public ApplicationNameAvailability {
        Objects.requireNonNull(tenantId, RequiredArgumentMessages.TENANT_ID);
        Objects.requireNonNull(name, RequiredArgumentMessages.APPLICATION_NAME);
    }
}
