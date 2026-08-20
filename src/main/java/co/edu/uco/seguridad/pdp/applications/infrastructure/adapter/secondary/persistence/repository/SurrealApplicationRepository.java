package co.edu.uco.seguridad.pdp.applications.infrastructure.adapter.secondary.persistence.repository;

import co.edu.uco.seguridad.pdp.applications.application.port.secondary.repository.ApplicationRepository;
import co.edu.uco.seguridad.pdp.applications.domain.Application;
import co.edu.uco.seguridad.pdp.applications.domain.ApplicationBaseUrl;
import co.edu.uco.seguridad.pdp.applications.infrastructure.adapter.secondary.persistence.schema.ApplicationSchema;
import co.edu.uco.seguridad.pdp.commons.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.ApplicationName;
import co.edu.uco.seguridad.pdp.commons.TenantId;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import co.edu.uco.seguridad.shared.persistence.surrealdb.SurrealDbClient;
import co.edu.uco.seguridad.shared.persistence.surrealdb.SurrealRecordId;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tools.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Adaptador real sobre SurrealDB (ADR-019). {@code existsByTenantAndName} es la puerta principal
 * contra duplicados; el índice {@code application_tenant_name} solo cierra la ventana de carrera
 * entre el chequeo y la escritura, no el mecanismo que produce el mensaje de error que ve el cliente.
 */
public final class SurrealApplicationRepository implements ApplicationRepository {

    private final SurrealDbClient client;

    public SurrealApplicationRepository(SurrealDbClient client) {
        this.client = Objects.requireNonNull(client, RequiredArgumentMessages.SURREALDB_CLIENT);
    }

    @Override
    public Mono<Boolean> existsByTenantAndName(TenantId tenantId, ApplicationName name) {
        return client.execute(
                        "SELECT id FROM %s WHERE tenantId = $tenantId AND name = $name LIMIT 1;"
                                .formatted(ApplicationSchema.TABLE),
                        Map.of("tenantId", tenantId.value(), "name", name.value()))
                .map(results -> !results.get(0).isEmpty());
    }

    @Override
    public Mono<Application> findByTenantAndId(TenantId tenantId, ApplicationId applicationId) {
        return client.execute(
                        "SELECT * FROM type::record('%s', $id) WHERE tenantId = $tenantId;"
                                .formatted(ApplicationSchema.TABLE),
                        Map.of("id", applicationId.value().toString(), "tenantId", tenantId.value()))
                .map(results -> results.get(0))
                .flatMap(rows -> rows.isEmpty() ? Mono.empty() : Mono.just(toDomain(rows.get(0))));
    }

    @Override
    public Flux<Application> findAllByTenant(TenantId tenantId) {
        return client.execute(
                        "SELECT * FROM %s WHERE tenantId = $tenantId ORDER BY registeredAt DESC;"
                                .formatted(ApplicationSchema.TABLE),
                        Map.of("tenantId", tenantId.value()))
                .flatMapMany(results -> Flux.fromIterable(results.get(0).valueStream().toList()))
                .map(SurrealApplicationRepository::toDomain);
    }

    @Override
    public Mono<Application> save(Application application) {
        return client.execute(
                        """
                        CREATE type::record('%s', $id) SET \
                        tenantId = $tenantId, name = $name, description = $description, baseUrl = $baseUrl, \
                        registeredAt = <datetime>$registeredAt;\
                        """.formatted(ApplicationSchema.TABLE),
                        Map.of(
                                "id", application.id().value().toString(),
                                "tenantId", application.tenantId().value(),
                                "name", application.name().value(),
                                "description", application.description(),
                                "baseUrl", application.baseUrl().value(),
                                "registeredAt", application.registeredAt().toString()))
                .thenReturn(application);
    }

    @Override
    public Mono<Void> deleteById(ApplicationId applicationId) {
        return client.execute(
                        "DELETE type::record('%s', $id);".formatted(ApplicationSchema.TABLE),
                        Map.of("id", applicationId.value().toString()))
                .then();
    }

    private static Application toDomain(JsonNode row) {
        return new Application(
                new ApplicationId(UUID.fromString(SurrealRecordId.idPart(row.path("id").asString()))),
                new TenantId(row.path("tenantId").asString()),
                new ApplicationName(row.path("name").asString()),
                row.path("description").asString(""),
                new ApplicationBaseUrl(row.path("baseUrl").asString()),
                Instant.parse(row.path("registeredAt").asString()));
    }
}
