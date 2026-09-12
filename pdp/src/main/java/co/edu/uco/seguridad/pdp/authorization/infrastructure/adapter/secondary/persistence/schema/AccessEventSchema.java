package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.secondary.persistence.schema;

/** Nombre de la tabla y del índice de respaldo. Nunca un literal suelto en la consulta. */
public final class AccessEventSchema {

    public static final String TABLE = "access_event";
    public static final String CORRELATION_INDEX = "access_event_correlation_id";

    private AccessEventSchema() {
    }
}
