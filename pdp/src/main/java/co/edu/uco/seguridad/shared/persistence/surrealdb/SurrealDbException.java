package co.edu.uco.seguridad.shared.persistence.surrealdb;

/**
 * SurrealDB rechazó una sentencia, o la petición no pudo interpretarse. No es una
 * {@code DomainException}: es un fallo técnico del almacén. Cae en el manejador genérico de
 * {@code ApiErrorHandler} — el detalle real va al log, nunca al cliente.
 */
public final class SurrealDbException extends RuntimeException {

    public SurrealDbException(String message) {
        super(message);
    }
}
