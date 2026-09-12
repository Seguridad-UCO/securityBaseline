package co.edu.uco.seguridad.shared.contract;

import reactor.core.publisher.Flux;

/**
 * Forma genérica reactiva con entrada y un flujo de resultados. Sin uso todavía: preparada para
 * cuando una operación tenga como resultado natural una secuencia, no un único valor.
 */
@FunctionalInterface
public interface ReactiveStreamOperation<I, O> {

    Flux<O> execute(I input);
}
