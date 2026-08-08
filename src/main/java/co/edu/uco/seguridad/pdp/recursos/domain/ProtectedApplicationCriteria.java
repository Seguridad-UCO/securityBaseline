package co.edu.uco.seguridad.pdp.recursos.domain;

import co.edu.uco.seguridad.pdp.commons.TenantId;

import java.util.Optional;

/**
 * Especificación para consultas de catálogo (criterios 15 a 17).
 *
 * <p>Cada filtro es opcional y un filtro ausente significa "no restringir", por lo que una operación
 * cubre todas las combinaciones y el repositorio nunca crece con un {@code findByX} por atributo. La
 * semántica de coincidencia vive aquí, en el dominio, para que el adaptador en memoria y un futuro adaptador
 * SurrealDB no puedan estar en desacuerdo sobre qué significa {@code contains}.</p>
 */
public record ProtectedApplicationCriteria(Optional<TenantId> tenantId,
                                           Optional<String> nameContains,
                                           Optional<String> resourceContains) {

    public ProtectedApplicationCriteria {
        tenantId = tenantId == null ? Optional.empty() : tenantId;
        nameContains = normalise(nameContains);
        resourceContains = normalise(resourceContains);
    }

    public static ProtectedApplicationCriteria unfiltered() {
        return new ProtectedApplicationCriteria(Optional.empty(), Optional.empty(), Optional.empty());
    }

    public boolean matches(ProtectedResource resource) {
        return tenantId.map(resource::belongsTo).orElse(true)
                && nameContains.map(resource.applicationName()::contains).orElse(true)
                && resourceContains.map(resource.code()::contains).orElse(true);
    }

    private static Optional<String> normalise(Optional<String> fragment) {
        return fragment == null
                ? Optional.empty()
                : fragment.map(String::trim).filter(value -> !value.isEmpty());
    }
}
