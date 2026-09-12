package co.edu.uco.seguridad.shared.observability;

import io.micrometer.observation.Observation;
import io.micrometer.common.KeyValues;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.contextpropagation.ObservationThreadLocalAccessor;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SignalType;

/** Una observación por suscripción, cerrada también ante error o cancelación. */
public final class ReactiveTelemetry {
    private ReactiveTelemetry() { }

    public static <T> Mono<T> observe(String operation, ObservationRegistry registry,
            Supplier<Mono<T>> work, BiConsumer<Observation, T> resultTags) {
        return observe(operation, registry, KeyValues.empty(), work, resultTags);
    }

    public static <T> Mono<T> observe(String operation, ObservationRegistry registry, KeyValues initialTags,
            Supplier<Mono<T>> work, BiConsumer<Observation, T> resultTags) {
        return Mono.deferContextual(context -> {
            Observation parent = context.getOrDefault(ObservationThreadLocalAccessor.KEY, null);
            Observation observation = Observation.createNotStarted(operation, registry)
                    .parentObservation(parent).lowCardinalityKeyValues(initialTags)
                    .lowCardinalityKeyValue("error.type", "none")
                    .lowCardinalityKeyValue("outcome", "success").start();
            return Mono.defer(work)
                    .doOnNext(value -> resultTags.accept(observation, value))
                    .doOnError(error -> {
                        observation.lowCardinalityKeyValue("outcome", "error");
                        observation.lowCardinalityKeyValue("error.type", error.getClass().getSimpleName());
                        observation.error(new TelemetryFailure(error.getClass().getSimpleName()));
                    })
                    .doFinally(signal -> {
                        if (signal == SignalType.CANCEL)
                            observation.lowCardinalityKeyValue("outcome", "cancelled");
                        try (Observation.Scope scope = observation.openScope()) {
                            LoggerFactory.getLogger(ReactiveTelemetry.class).atInfo()
                                    .addKeyValue("event.name", operation + ".completed")
                                    .addKeyValue("operation", operation)
                                    .addKeyValue("outcome", observation.getContext()
                                            .getLowCardinalityKeyValue("outcome").getValue())
                                    .addKeyValue("requestId", context.getOrDefault("requestId", ""))
                                    .addKeyValue("correlationId", context.getOrDefault("correlationId", ""))
                                    .log("Operación instrumentada finalizada");
                        } finally {
                            observation.stop();
                        }
                    })
                    .contextWrite(values -> values.put(ObservationThreadLocalAccessor.KEY, observation));
        });
    }

    private static final class TelemetryFailure extends RuntimeException {
        private TelemetryFailure(String type) {
            super(type, null, false, false);
        }
    }
}
