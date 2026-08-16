package co.edu.uco.seguridad.pdp.aplicaciones.infrastructure.adapter.secondary.persistence.schema;

/**
 * Nombre de tabla e índice de SurrealDB del módulo, en un único lugar que comparten el repositorio
 * y el inicializador de esquema — antes repetidos como literales en ambas clases.
 */
public final class ApplicationSchema {

    public static final String TABLE = "application";
    public static final String INDEX_TENANT_NAME = "application_tenant_name";

    private ApplicationSchema() {
    }
}
