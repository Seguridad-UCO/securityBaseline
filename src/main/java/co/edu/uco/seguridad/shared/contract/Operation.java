package co.edu.uco.seguridad.shared.contract;

/**
 * Forma genérica sincrónica con entrada y salida, sin E/S.
 *
 * <p>Base común para reglas, casos de uso, interactores y validadores de reglas en todas las capas
 * del proyecto: cualquier operación que solo necesite los datos que se le pasan para decidir y
 * producir un resultado tiene esta forma, sin importar en qué capa viva.</p>
 *
 * @param <I> la entrada de la operación
 * @param <O> el resultado que produce
 */
@FunctionalInterface
public interface Operation<I, O> {

    O execute(I input);
}
