package co.edu.uco.seguridad.shared.contract;

import reactor.core.publisher.Mono;

/**
 * Forma genérica reactiva con entrada y sin resultado significativo.
 *
 * <p>El {@code Mono<Void>} devuelto se completa vacío cuando la operación tiene éxito y señala su
 * propia excepción cuando no.</p>
 *
 * @param <I> la entrada de la operación
 */
@FunctionalInterface
public interface ReactiveOperationWithoutResult<I> {

    Mono<Void> execute(I input);
}
