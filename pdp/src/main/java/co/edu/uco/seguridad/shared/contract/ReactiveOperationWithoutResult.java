package co.edu.uco.seguridad.shared.contract;

import reactor.core.publisher.Mono;

/**
 * Forma genérica reactiva con entrada y sin resultado significativo.
 */
@FunctionalInterface
public interface ReactiveOperationWithoutResult<I> {

    Mono<Void> execute(I input);
}
