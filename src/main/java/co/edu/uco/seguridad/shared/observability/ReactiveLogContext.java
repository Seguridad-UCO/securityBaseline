package co.edu.uco.seguridad.shared.observability;

import org.slf4j.Logger;
import org.slf4j.MDC;
import reactor.core.publisher.Mono;
import java.util.function.Function;

/** Puente de Reactor Context a MDC solo mientras se ejecuta una declaración de registro. */
public final class ReactiveLogContext {
    private ReactiveLogContext() { }
    public static <T> Function<Mono<T>, Mono<T>> withContext(Logger log, String event) {
        return upstream -> Mono.deferContextual(context -> upstream
            .doOnSuccess(value -> log(log, context, event, "outcome=success"))
            .doOnError(error -> log(log, context, event, "outcome=error type=" + error.getClass().getSimpleName())));
    }
    private static void log(Logger logger, reactor.util.context.ContextView context, String event, String outcome) {
        try (MDC.MDCCloseable request = MDC.putCloseable("requestId", context.getOrDefault("requestId", ""));
             MDC.MDCCloseable correlation = MDC.putCloseable("correlationId", context.getOrDefault("correlationId", ""))) {
            logger.info("{} {}", event, outcome);
        }
    }
}
