package co.edu.uco.seguridad.shared.contract;

import reactor.core.publisher.Mono;

/**
 * Forma genérica reactiva con entrada y salida — para operaciones con E/S asíncrona.
 */
@FunctionalInterface
public interface ReactiveOperation<I, O> {

    Mono<O> execute(I input);
}
