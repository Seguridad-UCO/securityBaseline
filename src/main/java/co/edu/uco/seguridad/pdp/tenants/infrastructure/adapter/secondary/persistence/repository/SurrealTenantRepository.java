package co.edu.uco.seguridad.pdp.tenants.infrastructure.adapter.secondary.persistence.repository;

import co.edu.uco.seguridad.pdp.commons.TenantId;
import co.edu.uco.seguridad.pdp.tenants.application.secondaryport.repository.TenantRepository;
import co.edu.uco.seguridad.pdp.tenants.domain.Tenant;
import co.edu.uco.seguridad.pdp.tenants.infrastructure.adapter.secondary.persistence.entity.TenantEntity;
import co.edu.uco.seguridad.pdp.tenants.infrastructure.adapter.secondary.persistence.mapper.TenantPersistenceMapper;
import co.edu.uco.seguridad.pdp.tenants.infrastructure.adapter.secondary.persistence.schema.TenantSchema;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import co.edu.uco.seguridad.shared.persistence.surrealdb.SurrealDbClient;
import co.edu.uco.seguridad.shared.persistence.surrealdb.SurrealRecordId;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tools.jackson.databind.JsonNode;

import java.util.Map;
import java.util.Objects;

/**
 * Adaptador secundario (driven) real sobre SurrealDB (ADR-0004). Igual que el dummy que reemplaza,
 * almacena filas planas y traduce con {@link TenantPersistenceMapper}: sustituirlo no tocó
 * {@code domain/} ni {@code application/}.
 */
public final class SurrealTenantRepository implements TenantRepository {

    private final SurrealDbClient client;

    public SurrealTenantRepository(SurrealDbClient client) {
        this.client = Objects.requireNonNull(client, RequiredArgumentMessages.SURREALDB_CLIENT);
    }

    @Override
    public Mono<Tenant> findById(TenantId tenantId) {
        return client.execute(
                        "SELECT * FROM type::record('%s', $id);".formatted(TenantSchema.TABLE),
                        Map.of("id", tenantId.value()))
                .map(results -> results.get(0))
                .flatMap(rows -> rows.isEmpty() ? Mono.empty() : Mono.just(toDomain(rows.get(0))));
    }

    @Override
    public Mono<Boolean> existsById(TenantId tenantId) {
        return client.execute(
                        "SELECT id FROM type::record('%s', $id);".formatted(TenantSchema.TABLE),
                        Map.of("id", tenantId.value()))
                .map(results -> !results.get(0).isEmpty());
    }

    @Override
    public Mono<Tenant> save(Tenant tenant) {
        return client.execute(
                        "CREATE type::record('%s', $id) SET name = $name, status = $status;".formatted(TenantSchema.TABLE),
                        Map.of(
                                "id", tenant.id().value(),
                                "name", tenant.name().value(),
                                "status", tenant.status().name()))
                .thenReturn(tenant);
    }

    @Override
    public Flux<Tenant> findAll() {
        return client.execute("SELECT * FROM %s ORDER BY name ASC;".formatted(TenantSchema.TABLE), Map.of())
                .flatMapMany(results -> Flux.fromIterable(results.get(0).valueStream().toList()))
                .map(SurrealTenantRepository::toDomain);
    }

    private static Tenant toDomain(JsonNode row) {
        TenantEntity entity = new TenantEntity(
                SurrealRecordId.idPart(row.path("id").asString()),
                row.path("name").asString(),
                row.path("status").asString());
        return TenantPersistenceMapper.toDomain(entity);
    }
}
