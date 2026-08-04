package co.edu.uco.seguridad.applications.application.port.out;

import reactor.core.publisher.Mono;
import java.util.function.Supplier;

public interface ReactiveTransactionPort { <T> Mono<T> execute(Supplier<Mono<T>> work); }
