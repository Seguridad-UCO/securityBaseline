package co.edu.uco.seguridad.pep.application.contract;

import reactor.core.publisher.Mono;

/** Contrato reactivo para una operación de aplicación con entrada y salida. */
@FunctionalInterface
public interface ReactiveOperation<I, O> {
    Mono<O> execute(I input);
}
