package co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.secondary.persistence.schema;

import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import co.edu.uco.seguridad.shared.persistence.surrealdb.SurrealDbClient;
import co.edu.uco.seguridad.shared.persistence.surrealdb.SurrealSchemaInitializer;

import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Objects;

/**
 * Define la tabla {@code assignment}, siguiendo el patrón de {@code SurrealRoleSchemaInitializer}.
 * Sin índice único de respaldo: la unicidad de R4 depende del tiempo de ejecución (asignación
 * "vigente"), no de columnas estáticas — ver PLAN-HU-005 §5 y §11.3.
 */
public final class SurrealAssignmentSchemaInitializer extends SurrealSchemaInitializer {

    private final SurrealDbClient client;

    public SurrealAssignmentSchemaInitializer(SurrealDbClient client) {
        this.client = Objects.requireNonNull(client, RequiredArgumentMessages.SURREALDB_CLIENT);
    }

    @Override
    protected String module() {
        return "assignments";
    }

    @Override
    protected Mono<Void> defineSchema() {
        return client.ensureNamespaceAndDatabase()
                .then(client.execute(
                        "DEFINE TABLE IF NOT EXISTS %s SCHEMALESS;".formatted(AssignmentSchema.TABLE), Map.of()))
                .then();
    }
}
