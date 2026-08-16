package co.edu.uco.seguridad.pdp.recursos.domain;

import co.edu.uco.seguridad.crosscutting.messages.RequiredArgumentMessages;
import co.edu.uco.seguridad.pdp.commons.TenantId;

import java.util.Objects;
import java.util.Optional;

/**
 * Especificación para consultas de catálogo (criterios 15 a 17). {@code tenantId} es obligatorio:
 * toda consulta llega autenticada, y el tenant sale del token (ADR-018), no de un filtro omitible.
 */
public record ProtectedApplicationCriteria(TenantId tenantId,
                                           Optional<String> nameContains,
                                           Optional<String> resourceContains) {

    public ProtectedApplicationCriteria {
        Objects.requireNonNull(tenantId, RequiredArgumentMessages.TENANT_ID);
        nameContains = normalise(nameContains);
        resourceContains = normalise(resourceContains);
    }

    public static ProtectedApplicationCriteria scopedTo(TenantId tenantId) {
        return new ProtectedApplicationCriteria(tenantId, Optional.empty(), Optional.empty());
    }

    public boolean matches(ProtectedResource resource) {
        return resource.belongsTo(tenantId)
                && nameContains.map(resource.applicationName()::contains).orElse(true)
                && resourceContains.map(resource.code()::contains).orElse(true);
    }

    private static Optional<String> normalise(Optional<String> fragment) {
        return fragment.map(String::trim).filter(value -> !value.isEmpty());
    }
}
