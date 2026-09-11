package co.edu.uco.seguridad.pep.enforcement.infrastructure.adapter.secondary.http;

import co.edu.uco.seguridad.pep.enforcement.infrastructure.properties.PdpClientProperties;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.ReactiveHealthIndicator;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/** La disponibilidad del PDP afecta readiness, nunca liveness del PEP. */
@Component("pdp")
final class PdpReadinessIndicator implements ReactiveHealthIndicator {
    private final WebClient client;
    private final PdpClientProperties properties;

    PdpReadinessIndicator(WebClient pdpWebClient, PdpClientProperties properties) {
        this.client = pdpWebClient;
        this.properties = properties;
    }

    @Override
    public Mono<Health> health() {
        return client.get().uri("/actuator/health").exchangeToMono(response -> {
                    boolean available = response.statusCode().is2xxSuccessful();
                    return response.releaseBody().thenReturn((available ? Health.up() : Health.down()).build());
                }).timeout(properties.timeout()).doOnError(error ->
                        org.slf4j.LoggerFactory.getLogger(PdpReadinessIndicator.class).warn("PDP readiness failureType={} location={}",
                                error.getClass().getName(), error.getStackTrace().length == 0 ? "unknown" : error.getStackTrace()[0]))
                .onErrorReturn(Health.down().build());
    }
}
