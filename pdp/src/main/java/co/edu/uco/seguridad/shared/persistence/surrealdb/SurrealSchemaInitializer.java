package co.edu.uco.seguridad.shared.persistence.surrealdb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Base de los inicializadores de esquema de cada módulo.
 *
 * <p><strong>El arranque no depende de que la base de datos esté viva.</strong> Antes cada módulo
 * hacía {@code .block()} sin plazo dentro de su {@code ApplicationRunner}: si SurrealDB no
 * respondía, la aplicación no terminaba de arrancar nunca y App Service devolvía un 503 mudo
 * durante minutos, sin decir por qué. Una base de datos caída es un problema de la base de datos;
 * que además impida arrancar al proceso es un problema añadido y evitable.
 *
 * <p>Ahora se espera al esquema —para que esté listo antes de la primera petición cuando todo va
 * bien— pero con plazo, y un fallo se registra en vez de tumbar el contexto. Quien informa de que
 * la base está caída es {@link SurrealDbHealthIndicator}, que es su trabajo.
 */
public abstract class SurrealSchemaInitializer implements ApplicationRunner {

    /** Suficiente para una base sana y corto para no dejar el arranque colgado si no lo está. */
    private static final Duration STARTUP_TIMEOUT = Duration.ofSeconds(15);

    private static final Logger LOG = LoggerFactory.getLogger(SurrealSchemaInitializer.class);

    /** Las sentencias que definen y siembran el esquema del módulo. */
    protected abstract Mono<Void> defineSchema();

    /** El módulo al que pertenece este inicializador, para que el log diga cuál falló. */
    protected abstract String module();

    @Override
    public final void run(ApplicationArguments args) {
        defineSchema()
                .timeout(STARTUP_TIMEOUT)
                .doOnSuccess(ignored -> LOG.info("esquema de {} listo", module()))
                .doOnError(cause -> LOG.error(
                        "no se pudo preparar el esquema de {}: la aplicación arranca igualmente y "
                                + "el estado de la base se reporta en /actuator/health",
                        module(), cause))
                .onErrorComplete()
                .block();
    }
}
