package co.edu.uco.seguridad.pdp.aplicaciones.infrastructure.adapter.secondary.persistence.schema;

import co.edu.uco.seguridad.shared.persistence.surrealdb.SurrealDbClient;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;

import java.util.Map;
import java.util.Objects;

/**
 * Define la tabla {@code application} y su índice único de respaldo (ADR-0004). La regla de
 * aplicación ({@code ApplicationNameMustBeUniqueForTenantRule}) sigue siendo quien produce el
 * mensaje de error normal; este índice solo evita que una condición de carrera deje dos filas con el
 * mismo tenant y nombre si dos peticiones pasan el chequeo casi al mismo tiempo.
 */
public final class SurrealApplicationSchemaInitializer implements ApplicationRunner {

    private final SurrealDbClient client;

    public SurrealApplicationSchemaInitializer(SurrealDbClient client) {
        this.client = Objects.requireNonNull(client, "se requiere el cliente de SurrealDB");
    }

    @Override
    public void run(ApplicationArguments args) {
        client.ensureNamespaceAndDatabase()
                .then(client.execute(
                        """
                        DEFINE TABLE IF NOT EXISTS application SCHEMALESS;
                        DEFINE INDEX IF NOT EXISTS application_tenant_name ON application \
                        COLUMNS tenantId, name UNIQUE;\
                        """,
                        Map.of()))
                .then()
                .block();
    }
}
