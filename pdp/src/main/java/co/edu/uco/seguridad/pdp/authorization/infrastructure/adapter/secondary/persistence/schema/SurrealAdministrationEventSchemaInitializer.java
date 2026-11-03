package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.secondary.persistence.schema;

import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import co.edu.uco.seguridad.shared.persistence.surrealdb.SurrealDbClient;
import co.edu.uco.seguridad.shared.persistence.surrealdb.SurrealSchemaInitializer;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Objects;

/**
 * Define la tabla {@code administration_event} y su índice de respaldo sobre {@code correlationId}
 * (HU-021). No es único, mismo criterio que {@code access_event}.
 */
public final class SurrealAdministrationEventSchemaInitializer extends SurrealSchemaInitializer {

    private final SurrealDbClient client;

    public SurrealAdministrationEventSchemaInitializer(SurrealDbClient client) {
        this.client = Objects.requireNonNull(client, RequiredArgumentMessages.SURREALDB_CLIENT);
    }

    @Override
    protected String module() {
        return "authorization";
    }

    @Override
    protected Mono<Void> defineSchema() {
        return client.ensureNamespaceAndDatabase()
                .then(client.execute(
                        """
                                DEFINE TABLE IF NOT EXISTS %1$s SCHEMALESS;
                                DEFINE INDEX IF NOT EXISTS %2$s ON %1$s COLUMNS correlationId;\
                                """.formatted(AdministrationEventSchema.TABLE,
                                AdministrationEventSchema.CORRELATION_INDEX),
                        Map.of()))
                .then();
    }
}
