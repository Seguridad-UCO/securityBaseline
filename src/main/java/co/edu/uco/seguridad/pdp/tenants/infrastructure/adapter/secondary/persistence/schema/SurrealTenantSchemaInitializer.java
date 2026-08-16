package co.edu.uco.seguridad.pdp.tenants.infrastructure.adapter.secondary.persistence.schema;

import co.edu.uco.seguridad.crosscutting.messages.RequiredArgumentMessages;
import co.edu.uco.seguridad.pdp.tenants.infrastructure.properties.TenantCatalogProperties;
import co.edu.uco.seguridad.shared.persistence.surrealdb.SurrealDbClient;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import reactor.core.publisher.Flux;

import java.util.Map;
import java.util.Objects;

/**
 * Define la tabla {@code tenant} y siembra el catálogo configurado (ADR-0004) al arrancar.
 *
 * <p>{@code UPSERT} en vez de {@code CREATE}: el catálogo de tenants es configuración, no un dato
 * que el negocio escriba en tiempo de ejecución (no existe un caso de uso "registrar tenant"), así
 * que cada arranque vuelve a aplicar la fuente de verdad — {@code application.properties} — sin
 * fallar si el registro ya existía de un arranque anterior.</p>
 *
 * <p>Corre en el hilo principal de arranque, antes de que Netty acepte tráfico, así que bloquear
 * aquí es seguro: no es el hilo de un event loop reactivo atendiendo una petición real.</p>
 */
public final class SurrealTenantSchemaInitializer implements ApplicationRunner {

    private final SurrealDbClient client;
    private final TenantCatalogProperties properties;

    public SurrealTenantSchemaInitializer(SurrealDbClient client, TenantCatalogProperties properties) {
        this.client = Objects.requireNonNull(client, RequiredArgumentMessages.SURREALDB_CLIENT);
        this.properties = Objects.requireNonNull(properties, RequiredArgumentMessages.TENANT_CATALOG);
    }

    @Override
    public void run(ApplicationArguments args) {
        client.ensureNamespaceAndDatabase()
                .then(client.execute(
                        "DEFINE TABLE IF NOT EXISTS %s SCHEMALESS;".formatted(TenantSchema.TABLE), Map.of()))
                .thenMany(Flux.fromIterable(properties.seed().entrySet()))
                .concatMap(entry -> client.execute(
                        "UPSERT type::record('%s', $id) SET status = $status;".formatted(TenantSchema.TABLE),
                        Map.of("id", entry.getKey(), "status", entry.getValue())))
                .then()
                .block();
    }
}
