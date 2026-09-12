package co.edu.uco.seguridad.pdp.tenants.domain.rule.model;

import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.tenants.domain.model.TenantStatus;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;

import java.util.Objects;

/**
 * Entrada ya resuelta de {@code TenantStatusMustBeActiveRule}: el estado actual del inquilino.
 *
 * <p>Lleva el estado y no el {@link Tenant} completo a propósito: es lo único sobre lo que la regla
 * decide, y así el puerto puede responder con una proyección en vez de cargar el agregado entero.</p>
 */
public record TenantActivation(TenantId tenantId, TenantStatus status) {

    public TenantActivation {
        Objects.requireNonNull(tenantId, RequiredArgumentMessages.TENANT_ID);
        Objects.requireNonNull(status, RequiredArgumentMessages.TENANT_STATUS);
    }
}
