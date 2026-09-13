package co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.secondary.persistence.schema;

import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import co.edu.uco.seguridad.shared.persistence.surrealdb.SurrealDbClient;
import co.edu.uco.seguridad.shared.persistence.surrealdb.SurrealSchemaInitializer;

import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Objects;

/** Define la tabla {@code profile_assignment}, siguiendo el patrón de {@code SurrealAssignmentSchemaInitializer}. */
public final class SurrealProfileAssignmentSchemaInitializer extends SurrealSchemaInitializer {

    private final SurrealDbClient client;

    public SurrealProfileAssignmentSchemaInitializer(SurrealDbClient client) {
        this.client = Objects.requireNonNull(client, RequiredArgumentMessages.SURREALDB_CLIENT);
    }

    @Override
    protected String module() {
        return "assignments";
    }

    @Override
    protected Mono<Void> defineSchema() {
        return client.ensureNamespaceAndDatabase()
                .then(client.execute("DEFINE TABLE IF NOT EXISTS %s SCHEMALESS;".formatted(ProfileAssignmentSchema.TABLE),
                        Map.of()))
                .then();
    }
}
