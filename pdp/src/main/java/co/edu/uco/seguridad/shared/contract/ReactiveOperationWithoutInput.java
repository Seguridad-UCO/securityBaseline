package co.edu.uco.seguridad.shared.contract;

import reactor.core.publisher.Mono;

/** Forma genérica reactiva sin entrada, con resultado — para operaciones sin datos del llamador. */
@FunctionalInterface
public interface ReactiveOperationWithoutInput<O> {

    Mono<O> execute();
}
