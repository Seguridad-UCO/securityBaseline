package co.edu.uco.seguridad.shared.contract;

import reactor.core.publisher.Mono;

/**
 * Forma genérica reactiva sin entrada, con resultado.
 *
 * <p>Para operaciones que no necesitan ningún dato del llamador — por ejemplo, una consulta que
 * siempre parte del mismo estado inicial.</p>
 *
 * @param <O> el resultado que produce
 */
@FunctionalInterface
public interface ReactiveOperationWithoutInput<O> {

    Mono<O> execute();
}
