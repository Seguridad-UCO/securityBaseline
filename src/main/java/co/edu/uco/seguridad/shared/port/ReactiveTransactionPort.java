package co.edu.uco.seguridad.shared.port;

import reactor.core.publisher.Mono;

import java.util.function.Supplier;

/**
 * Límite transaccional expresado como un puerto, para que los casos de uso delimiten una unidad de trabajo sin
 * saber qué tecnología la aplica.
 *
 * <p>El trabajo se pasa como un {@link Supplier} en lugar de un {@code Mono} ya ensamblado para que
 * el adaptador decida cuándo ocurre la suscripción y pueda tomar una instantánea de antemano.</p>
 */
public interface ReactiveTransactionPort {

    <T> Mono<T> execute(Supplier<Mono<T>> work);
}
