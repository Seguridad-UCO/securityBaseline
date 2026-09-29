package co.edu.uco.seguridad.pdp.roles.infrastructure.adapter.secondary.persistence.schema;

/**
 * Nombre de la tabla. Nunca un literal suelto en la consulta.
 */
public final class RoleSchema {

    public static final String TABLE = "role";
    public static final String INDEX_SCOPE_NAME = "role_scope_name";

    private RoleSchema() {
    }
}
