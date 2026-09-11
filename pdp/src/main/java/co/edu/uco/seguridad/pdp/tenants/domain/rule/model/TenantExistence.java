package co.edu.uco.seguridad.pdp.tenants.domain.rule.model;

import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;

import java.util.Objects;

/**
 * Entrada ya resuelta de {@code TenantMustExistRule}: qué inquilino se preguntó y si el almacén lo
 * tenía. La consulta la hace quien puede hacerla —el validador de aplicación—; aquí solo llega la
 * respuesta, que es lo que permite que la regla sea una función pura.
 */
public record TenantExistence(TenantId tenantId, boolean registered) {

    public TenantExistence {
        Objects.requireNonNull(tenantId, RequiredArgumentMessages.TENANT_ID);
    }
}
