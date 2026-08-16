package co.edu.uco.seguridad.pdp.recursos.infrastructure.adapter.secondary.persistence.schema;

/**
 * Nombre de tabla e índice de SurrealDB del módulo, en un único lugar que comparten el repositorio
 * y el inicializador de esquema — antes repetidos como literales en ambas clases.
 */
public final class ProtectedResourceSchema {

    public static final String TABLE = "protected_resource";
    public static final String INDEX_GRANT = "protected_resource_grant";

    private ProtectedResourceSchema() {
    }
}
