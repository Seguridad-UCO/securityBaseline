package co.edu.uco.seguridad.pdp.profiles.infrastructure.adapter.secondary.persistence.schema;

/** Nombre de la tabla. Nunca un literal suelto en la consulta. */
public final class ProfileSchema {

    public static final String TABLE = "profile";
    public static final String INDEX_SCOPE_NAME = "profile_scope_name";

    private ProfileSchema() {
    }
}
