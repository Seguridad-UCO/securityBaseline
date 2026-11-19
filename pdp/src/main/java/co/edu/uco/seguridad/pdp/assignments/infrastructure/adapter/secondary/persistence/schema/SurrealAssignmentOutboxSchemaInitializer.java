package co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.secondary.persistence.schema;

import co.edu.uco.seguridad.shared.persistence.surrealdb.SurrealDbClient;
import co.edu.uco.seguridad.shared.persistence.surrealdb.SurrealSchemaInitializer;
import reactor.core.publisher.Mono;
import java.util.Map;

public final class SurrealAssignmentOutboxSchemaInitializer extends SurrealSchemaInitializer {
    private final SurrealDbClient client;
    public SurrealAssignmentOutboxSchemaInitializer(SurrealDbClient client) { this.client = client; }
    @Override protected String module() { return "assignment-outbox"; }
    @Override protected Mono<Void> defineSchema() {
        return client.ensureNamespaceAndDatabase().then(client.execute(
                "DEFINE TABLE IF NOT EXISTS %s SCHEMALESS;".formatted(AssignmentOutboxSchema.TABLE), Map.of())).then();
    }
}
