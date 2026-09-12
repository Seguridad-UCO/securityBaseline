package co.edu.uco.seguridad.pdp.roles.infrastructure.adapter.secondary.persistence.schema;

import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import co.edu.uco.seguridad.shared.persistence.surrealdb.SurrealDbClient;
import co.edu.uco.seguridad.shared.persistence.surrealdb.SurrealSchemaInitializer;

import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Objects;

/**
 * Define la tabla {@code role} y su índice único de respaldo sobre (level, tenantId, applicationId,
 * name), siguiendo el patrón de {@code SurrealProtectedResourceSchemaInitializer}.
 * {@code RoleNameMustBeUniqueInScopeRule} sigue siendo quien produce el mensaje de error normal;
 * este índice solo cierra la ventana de carrera entre el chequeo y la escritura.
 */
public final class SurrealRoleSchemaInitializer extends SurrealSchemaInitializer {

    private final SurrealDbClient client;

    public SurrealRoleSchemaInitializer(SurrealDbClient client) {
        this.client = Objects.requireNonNull(client, RequiredArgumentMessages.SURREALDB_CLIENT);
    }

    @Override
    protected String module() {
        return "roles";
    }

    @Override
    protected Mono<Void> defineSchema() {
        return client.ensureNamespaceAndDatabase()
                .then(client.execute(
                        """
                        DEFINE TABLE IF NOT EXISTS %1$s SCHEMALESS;
                        DEFINE INDEX IF NOT EXISTS %2$s ON %1$s \
                        COLUMNS level, tenantId, applicationId, name UNIQUE;\
                        """.formatted(RoleSchema.TABLE, RoleSchema.INDEX_SCOPE_NAME),
                        Map.of()))
                .then();
    }
}
