package co.edu.uco.seguridad.pdp.resources.infrastructure.adapter.secondary.persistence.schema;

import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import co.edu.uco.seguridad.shared.persistence.surrealdb.SurrealDbClient;
import co.edu.uco.seguridad.shared.persistence.surrealdb.SurrealSchemaInitializer;

import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Objects;

/**
 * Define la tabla {@code protected_resource} y su índice único de respaldo (ADR-0004).
 * {@code ProtectedResourceMustBeUniqueRule} sigue siendo quien produce el mensaje de error normal;
 * este índice solo cierra la ventana de carrera entre el chequeo y la escritura.
 */
public final class SurrealProtectedResourceSchemaInitializer extends SurrealSchemaInitializer {

    private final SurrealDbClient client;

    public SurrealProtectedResourceSchemaInitializer(SurrealDbClient client) {
        this.client = Objects.requireNonNull(client, RequiredArgumentMessages.SURREALDB_CLIENT);
    }

    @Override
    protected String module() {
        return "resources";
    }

    @Override
    protected Mono<Void> defineSchema() {
        return client.ensureNamespaceAndDatabase()
                .then(client.execute(
                        """
                        DEFINE TABLE IF NOT EXISTS %1$s SCHEMALESS;
                        DEFINE INDEX IF NOT EXISTS %2$s ON %1$s \
                        COLUMNS applicationId, path, method UNIQUE;\
                        """.formatted(ProtectedResourceSchema.TABLE, ProtectedResourceSchema.INDEX_GRANT),
                        Map.of()))
                .then();
    }
}
