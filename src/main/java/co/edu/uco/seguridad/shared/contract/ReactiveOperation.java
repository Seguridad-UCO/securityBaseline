package co.edu.uco.seguridad.shared.contract;

import reactor.core.publisher.Mono;

/**
 * Forma genérica reactiva con entrada y salida.
 *
 * <p>Para operaciones que deben consultar un repositorio, otro módulo, o cualquier colaborador
 * asíncrono antes de producir su resultado.</p>
 *
 * @param <I> la entrada de la operación
 * @param <O> el resultado que produce
 */
@FunctionalInterface
public interface ReactiveOperation<I, O> {

    Mono<O> execute(I input);
}
