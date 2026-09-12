package co.edu.uco.seguridad.shared.observability;

import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.MDC;
import reactor.core.publisher.Mono;

/** Puente de Reactor Context a MDC solo mientras se ejecuta una declaración de registro. */
public final class ReactiveLogContext {
    private ReactiveLogContext() { }
    public static <T> Function<Mono<T>, Mono<T>> withContext(Logger log, String event) {
        return upstream -> Mono.deferContextual(context -> upstream
            .doOnSuccess(value -> log(log, context, event, "success", "none"))
            .doOnError(error -> log(log, context, event, "error", error.getClass().getSimpleName())));
    }
    private static void log(Logger logger, reactor.util.context.ContextView context, String event, String outcome, String errorType) {
        try (MDC.MDCCloseable request = MDC.putCloseable("requestId", context.getOrDefault("requestId", ""));
             MDC.MDCCloseable correlation = MDC.putCloseable("correlationId", context.getOrDefault("correlationId", ""))) {
            logger.atInfo().addKeyValue("event.name", event).addKeyValue("operation", event)
                    .addKeyValue("outcome", outcome).addKeyValue("error.type", errorType)
                    .log("Operación finalizada");
        }
    }
}
