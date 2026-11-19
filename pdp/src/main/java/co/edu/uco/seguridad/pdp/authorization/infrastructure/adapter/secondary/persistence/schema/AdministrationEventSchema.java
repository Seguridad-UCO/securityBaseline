package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.secondary.persistence.schema;

/**
 * Nombre de la tabla de auditoría de operaciones administrativas (HU-021). Nunca un literal suelto.
 */
public final class AdministrationEventSchema {

    public static final String TABLE = "administration_event";
    public static final String CORRELATION_INDEX = "administration_event_correlation_id";

    private AdministrationEventSchema() {
    }
}
