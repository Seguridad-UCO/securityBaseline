package co.edu.uco.seguridad.pdp.applications.infrastructure.adapter.secondary.persistence.schema;

import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import co.edu.uco.seguridad.shared.persistence.surrealdb.SurrealDbClient;
import co.edu.uco.seguridad.shared.persistence.surrealdb.SurrealSchemaInitializer;

import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Objects;

/**
 * Define la tabla {@code application} y su índice único de respaldo (ADR-0004). La regla de
 * aplicación ({@code ApplicationNameMustBeUniqueForTenantRule}) sigue siendo quien produce el
 * mensaje de error normal; este índice solo evita que una condición de carrera deje dos filas con el
 * mismo tenant y nombre si dos peticiones pasan el chequeo casi al mismo tiempo.
 */
public final class SurrealApplicationSchemaInitializer extends SurrealSchemaInitializer {

    private final SurrealDbClient client;

    public SurrealApplicationSchemaInitializer(SurrealDbClient client) {
        this.client = Objects.requireNonNull(client, RequiredArgumentMessages.SURREALDB_CLIENT);
    }

    @Override
    protected String module() {
        return "applications";
    }

    @Override
    protected Mono<Void> defineSchema() {
        return client.ensureNamespaceAndDatabase()
                .then(client.execute(
                        """
                                DEFINE TABLE IF NOT EXISTS %1$s SCHEMALESS;
                                DEFINE INDEX IF NOT EXISTS %2$s ON %1$s \
                                COLUMNS tenantId, name UNIQUE;\
                                """.formatted(ApplicationSchema.TABLE, ApplicationSchema.INDEX_TENANT_NAME),
                        Map.of()))
                .then();
    }
}
