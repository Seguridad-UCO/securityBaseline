package co.edu.uco.seguridad.shared.contract;

/**
 * Forma genérica sincrónica con entrada y sin salida, sin E/S.
 *
 * <p>Sin repositorio, sin reloj, sin llamada remota — la operación o no dice nada o lanza su propia
 * excepción de dominio o de negocio.</p>
 *
 * @param <I> la entrada de la operación
 */
@FunctionalInterface
public interface OperationWithoutResult<I> {

    void execute(I input);
}
