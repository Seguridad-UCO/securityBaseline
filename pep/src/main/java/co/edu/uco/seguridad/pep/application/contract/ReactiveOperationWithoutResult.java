package co.edu.uco.seguridad.pep.application.contract;

import reactor.core.publisher.Mono;

/**
 * Contrato reactivo para una operación de aplicación que termina sin resultado.
 */
@FunctionalInterface
public interface ReactiveOperationWithoutResult<I> {
    Mono<Void> execute(I input);
}
