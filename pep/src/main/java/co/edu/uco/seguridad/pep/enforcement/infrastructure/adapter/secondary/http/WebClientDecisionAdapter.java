package co.edu.uco.seguridad.pep.enforcement.infrastructure.adapter.secondary.http;

import co.edu.uco.seguridad.pep.commons.AccessDecision;
import co.edu.uco.seguridad.pep.commons.EnforcementFailure;
import co.edu.uco.seguridad.pep.enforcement.application.port.primary.dto.request.EnforceAccessRequest;
import co.edu.uco.seguridad.pep.enforcement.application.port.secondary.DecisionPort;
import co.edu.uco.seguridad.pep.enforcement.infrastructure.properties.PdpClientProperties;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import static co.edu.uco.seguridad.pep.commons.EnforcementFailure.Kind.UNAUTHENTICATED;
import static co.edu.uco.seguridad.pep.commons.EnforcementFailure.Kind.UNAVAILABLE;

/** Adaptador secundario: traduce el puerto de decisión a la API HTTP v1 del PDP. */
public final class WebClientDecisionAdapter implements DecisionPort {
    private final WebClient client;
    private final PdpClientProperties properties;
    private final MeterRegistry metrics;

    public WebClientDecisionAdapter(WebClient client, PdpClientProperties properties, MeterRegistry metrics) {
        this.client = client;
        this.properties = properties;
        this.metrics = metrics;
    }

    @Override
    public Mono<AccessDecision> execute(EnforceAccessRequest input) {
        return Mono.defer(() -> {
            var request = input.accessRequest();
            long started = System.nanoTime();
            return client.post().uri("/internal/v1/access-decisions")
                    .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
                    .headers(h -> {
                        h.setBearerAuth(input.identityEvidence().bearer());
                        h.set("X-Request-Id", request.requestId());
                        h.set("X-Correlation-Id", request.correlationId());
                    }).bodyValue(request)
                    .exchangeToMono(response -> {
                        if (response.statusCode().value() == 401) {
                            return response.releaseBody().then(Mono.error(new EnforcementFailure(UNAUTHENTICATED, "TOKEN_INVALID")));
                        }
                        if (response.statusCode().value() != 200 || response.headers().contentType()
                                .filter(MediaType.APPLICATION_JSON::isCompatibleWith).isEmpty()) {
                            return response.releaseBody().then(Mono.error(new EnforcementFailure(UNAVAILABLE, "PDP_PROTOCOL_ERROR")));
                        }
                        return response.bodyToMono(AccessDecision.class);
                    }).timeout(properties.timeout())
                    .onErrorMap(error -> !(error instanceof EnforcementFailure),
                            error -> new EnforcementFailure(UNAVAILABLE, "PDP_UNAVAILABLE"))
                    .doOnNext(d -> metrics.counter("pep.pdp.decisions", "decision",
                            d.decision() == null ? "INVALID" : d.decision().name()).increment())
                    .doFinally(signal -> metrics.timer("pep.pdp.duration")
                            .record(System.nanoTime() - started, java.util.concurrent.TimeUnit.NANOSECONDS));
        });
    }
}
