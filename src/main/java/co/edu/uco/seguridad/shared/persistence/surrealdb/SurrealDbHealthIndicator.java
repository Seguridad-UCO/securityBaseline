package co.edu.uco.seguridad.shared.persistence.surrealdb;

import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.ReactiveHealthIndicator;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;

/**
 * Reporta si SurrealDB responde.
 *
 * <p>Sin esto, una base caída se manifestaba como un 503 sin explicación: el despliegue fallaba tras
 * treinta intentos y había que adivinar entre «el contenedor no arrancó», «el runtime está mal» y
 * «la base no es alcanzable». Ahora {@code /actuator/health} lo dice en la primera respuesta.
 *
 * <p>El estado sigue siendo {@code DOWN} cuando la base no responde, así que un despliegue contra
 * una base caída sigue fallando — que es lo correcto. Lo que cambia es que falla **diciendo por qué**.
 */
public final class SurrealDbHealthIndicator implements ReactiveHealthIndicator {

    private static final Duration PROBE_TIMEOUT = Duration.ofSeconds(5);

    private final SurrealDbClient client;
    private final SurrealDbProperties properties;

    public SurrealDbHealthIndicator(SurrealDbClient client, SurrealDbProperties properties) {
        this.client = Objects.requireNonNull(client);
        this.properties = Objects.requireNonNull(properties);
    }

    @Override
    public Mono<Health> health() {
        return client.execute("RETURN 1;", Map.of())
                .timeout(PROBE_TIMEOUT)
                .map(ignored -> Health.up().withDetail("url", properties.url()).build())
                .onErrorResume(cause -> Mono.just(Health.down()
                        .withDetail("url", properties.url())
                        .withDetail("reason", cause.getClass().getSimpleName() + ": " + cause.getMessage())
                        .build()));
    }
}
