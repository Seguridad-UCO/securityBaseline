package co.edu.uco.seguridad.pdp.tenants.infrastructure.adapter.secondary.persistence.schema;

/**
 * Nombre de tabla SurrealDB del módulo, en un único lugar que comparten el repositorio y el
 * inicializador de esquema — antes repetido como literal en ambas clases.
 */
public final class TenantSchema {

    public static final String TABLE = "tenant";

    private TenantSchema() {
    }
}
