package co.edu.uco.seguridad.pdp.recursos.domain;

import co.edu.uco.seguridad.pdp.commons.TenantId;

import java.util.Objects;
import java.util.Optional;

/**
 * Especificación para consultas de catálogo (criterios 15 a 17).
 *
 * <p>{@code tenantId} es obligatorio y no un filtro más: desde ADR-0003 toda consulta llega
 * autenticada, y el tenant sale del token, no de un parámetro que el llamador pueda omitir para ver
 * el catálogo completo. Los demás filtros siguen siendo opcionales — un filtro ausente significa "no
 * restringir" dentro del propio tenant — por lo que una operación cubre todas las combinaciones y el
 * repositorio nunca crece con un {@code findByX} por atributo. La semántica de coincidencia vive
 * aquí, en el dominio, para que el adaptador en memoria y un futuro adaptador SurrealDB no puedan
 * estar en desacuerdo sobre qué significa {@code contains}.</p>
 */
public record ProtectedApplicationCriteria(TenantId tenantId,
                                           Optional<String> nameContains,
                                           Optional<String> resourceContains) {

    public ProtectedApplicationCriteria {
        Objects.requireNonNull(tenantId, "se requiere id de inquilino");
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
