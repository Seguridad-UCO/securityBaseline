package co.edu.uco.seguridad.shared.contract;

import reactor.core.publisher.Flux;

/**
 * Forma genérica reactiva con entrada y un flujo de resultados.
 *
 * <p>Para operaciones cuyo resultado natural es una secuencia en lugar de un único valor —
 * este proyecto no la usa todavía, pero está aquí para que ninguna capa futura tenga que inventar
 * su propio contrato ad hoc cuando la necesite.</p>
 *
 * @param <I> la entrada de la operación
 * @param <O> el tipo de cada elemento del flujo
 */
@FunctionalInterface
public interface ReactiveStreamOperation<I, O> {

    Flux<O> execute(I input);
}
