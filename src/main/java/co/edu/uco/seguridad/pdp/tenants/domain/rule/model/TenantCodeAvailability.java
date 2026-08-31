package co.edu.uco.seguridad.pdp.tenants.domain.rule.model;

import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;

import java.util.Objects;

/** Entrada ya resuelta de {@code TenantCodeMustBeUniqueRule}: si el id ya estaba tomado. */
public record TenantCodeAvailability(TenantId tenantId, boolean alreadyRegistered) {

    public TenantCodeAvailability {
        Objects.requireNonNull(tenantId, RequiredArgumentMessages.TENANT_ID);
    }
}
