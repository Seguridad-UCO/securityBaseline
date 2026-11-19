package co.edu.uco.seguridad.pdp.authorization.infrastructure.serviceidentity;

import co.edu.uco.seguridad.shared.persistence.surrealdb.SurrealDbClient;
import co.edu.uco.seguridad.shared.persistence.surrealdb.SurrealSchemaInitializer;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Esquema local del agregado M2M; no mezcla los grants de máquinas con asignaciones humanas.
 */
public final class SurrealServiceIdentitySchemaInitializer extends SurrealSchemaInitializer {
    private final SurrealDbClient client;

    public SurrealServiceIdentitySchemaInitializer(SurrealDbClient client) {
        this.client = client;
    }

    @Override
    protected String module() {
        return "authorization-service-identities";
    }

    @Override
    protected Mono<Void> defineSchema() {
        return client.ensureNamespaceAndDatabase().then(client.execute("""
                DEFINE TABLE IF NOT EXISTS service_identity SCHEMALESS;
                DEFINE TABLE IF NOT EXISTS service_access_grant SCHEMALESS;
                DEFINE TABLE IF NOT EXISTS service_technical_role SCHEMALESS;
                DEFINE TABLE IF NOT EXISTS service_technical_role_resource SCHEMALESS;
                UPDATE service_access_grant SET tenantId = (SELECT VALUE tenantId FROM type::record('service_identity', serviceIdentityId) LIMIT 1)[0] WHERE tenantId IS NONE;
                """, Map.of())).then();
    }
}
