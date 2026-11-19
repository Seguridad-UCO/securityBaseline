package co.edu.uco.seguridad.shared.contract;

/**
 * Forma genérica sincrónica con entrada y sin salida, sin E/S — o no dice nada o lanza su excepción.
 */
@FunctionalInterface
public interface OperationWithoutResult<I> {

    void execute(I input);
}
