package co.edu.uco.seguridad.pdp.recursos.infrastructure.adapter.secondary.persistence.schema;

import co.edu.uco.seguridad.shared.persistence.surrealdb.SurrealDbClient;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Objects;

/**
 * Define la tabla {@code protected_resource} y su índice único de respaldo (ADR-0004).
 * {@code ProtectedResourceMustBeUniqueRule} sigue siendo quien produce el mensaje de error normal;
 * este índice solo cierra la ventana de carrera entre el chequeo y la escritura.
 */
public final class SurrealProtectedResourceSchemaInitializer implements ApplicationRunner {

    private final SurrealDbClient client;

    public SurrealProtectedResourceSchemaInitializer(SurrealDbClient client) {
        this.client = Objects.requireNonNull(client, "se requiere el cliente de SurrealDB");
    }

    @Override
    public void run(ApplicationArguments args) {
        client.ensureNamespaceAndDatabase()
                .then(client.execute(
                        """
                        DEFINE TABLE IF NOT EXISTS protected_resource SCHEMALESS;
                        DEFINE INDEX IF NOT EXISTS protected_resource_grant ON protected_resource \
                        COLUMNS applicationId, resourceCode, action UNIQUE;\
                        """,
                        Map.of()))
                .then()
                .block();
    }
}
