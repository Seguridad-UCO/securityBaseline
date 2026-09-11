package co.edu.uco.seguridad.pdp.tenants.infrastructure.adapter.secondary.persistence.schema;

import co.edu.uco.seguridad.pdp.tenants.infrastructure.properties.TenantCatalogProperties;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import co.edu.uco.seguridad.shared.persistence.surrealdb.SurrealDbClient;
import co.edu.uco.seguridad.shared.persistence.surrealdb.SurrealSchemaInitializer;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Objects;

/**
 * Define la tabla {@code tenant} y siembra el catálogo configurado (ADR-0004) al arrancar.
 *
 * <p>{@code UPSERT} en vez de {@code CREATE}: los tenants sembrados aquí son configuración de
 * arranque (ambientes de desarrollo, demos), no el único punto de alta — {@code CreateTenantUseCase}
 * cubre el alta en tiempo de ejecución. UPSERT deja que cada arranque reaplique esta semilla sin
 * fallar si el registro ya existía, y sin pisar un tenant creado en tiempo de ejecución con el mismo
 * código salvo que la semilla también lo declare.</p>
 *
 * <p>Corre en el hilo principal de arranque, antes de que Netty acepte tráfico, así que bloquear
 * aquí es seguro: no es el hilo de un event loop reactivo atendiendo una petición real.</p>
 */
public final class SurrealTenantSchemaInitializer extends SurrealSchemaInitializer {

    private final SurrealDbClient client;
    private final TenantCatalogProperties properties;

    public SurrealTenantSchemaInitializer(SurrealDbClient client, TenantCatalogProperties properties) {
        this.client = Objects.requireNonNull(client, RequiredArgumentMessages.SURREALDB_CLIENT);
        this.properties = Objects.requireNonNull(properties, RequiredArgumentMessages.TENANT_CATALOG);
    }

    @Override
    protected String module() {
        return "tenants";
    }

    @Override
    protected Mono<Void> defineSchema() {
        return client.ensureNamespaceAndDatabase()
                .then(client.execute(
                        "DEFINE TABLE IF NOT EXISTS %s SCHEMALESS;".formatted(TenantSchema.TABLE), Map.of()))
                .thenMany(Flux.fromIterable(properties.seed().entrySet()))
                .concatMap(entry -> client.execute(
                        "UPSERT type::record('%s', $id) SET status = $status, name = $name;".formatted(TenantSchema.TABLE),
                        Map.of("id", entry.getKey(), "status", entry.getValue(), "name", displayName(entry.getKey()))))
                .then();
    }

    private static String displayName(String code) {
        if ("universidad-uco".equals(code)) return "Universidad UCO";
        return code.replace('-', ' ');
    }
}
