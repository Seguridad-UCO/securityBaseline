package co.edu.uco.seguridad.shared.persistence.surrealdb;

/**
 * SurrealDB rechazó una sentencia o la petición completa no pudo interpretarse. No es una
 * {@code DomainException}: es un fallo técnico del almacén, no una decisión de negocio — igual que
 * {@code RequestContractException} no es una excepción de dominio porque describe la forma de HTTP,
 * no del negocio. {@code ApiErrorHandler} la deja caer en su manejador genérico: el detalle real va
 * al log, nunca al cliente.
 */
public final class SurrealDbException extends RuntimeException {

    public SurrealDbException(String message) {
        super(message);
    }
}
