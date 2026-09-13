package co.edu.uco.seguridad.pdp.resources.infrastructure.adapter.secondary.persistence.repository;

import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.ResourceId;
import co.edu.uco.seguridad.pdp.resources.application.secondaryport.repository.ProtectedResourceRepository;
import co.edu.uco.seguridad.pdp.resources.domain.model.HttpVerb;
import co.edu.uco.seguridad.pdp.resources.domain.ProtectedResource;
import co.edu.uco.seguridad.pdp.resources.domain.model.ResourcePath;
import co.edu.uco.seguridad.pdp.resources.infrastructure.adapter.secondary.persistence.entity.ProtectedResourceEntity;
import co.edu.uco.seguridad.pdp.resources.infrastructure.adapter.secondary.persistence.mapper.ProtectedResourcePersistenceMapper;
import co.edu.uco.seguridad.pdp.resources.infrastructure.adapter.secondary.persistence.schema.ProtectedResourceSchema;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import co.edu.uco.seguridad.shared.persistence.surrealdb.SurrealDbClient;
import co.edu.uco.seguridad.shared.persistence.surrealdb.SurrealRecordId;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tools.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * Adaptador secundario (driven) real sobre SurrealDB (ADR-0004).
 */
public final class SurrealProtectedResourceRepository implements ProtectedResourceRepository {

    private final SurrealDbClient client;

    public SurrealProtectedResourceRepository(SurrealDbClient client) {
        this.client = Objects.requireNonNull(client, RequiredArgumentMessages.SURREALDB_CLIENT);
    }

    @Override
    public Mono<Boolean> existsByApplicationPathAndMethod(ApplicationId applicationId, ResourcePath path,
            HttpVerb method) {
        return client.execute(
                        "SELECT id FROM %s WHERE applicationId = $applicationId AND path = $path AND method = $method LIMIT 1;"
                                .formatted(ProtectedResourceSchema.TABLE),
                        Map.of("applicationId", applicationId.value().toString(), "path", path.value(),
                                "method", method.name()))
                .map(results -> !results.get(0).isEmpty());
    }

    @Override
    public Flux<ProtectedResource> findAllByApplication(ApplicationId applicationId) {
        return client.execute(
                        "SELECT * FROM %s WHERE applicationId = $applicationId ORDER BY registeredAt DESC;"
                                .formatted(ProtectedResourceSchema.TABLE),
                        Map.of("applicationId", applicationId.value().toString()))
                .flatMapMany(results -> Flux.fromIterable(results.get(0).valueStream().toList()))
                .map(SurrealProtectedResourceRepository::toDomain);
    }

    @Override
    public Mono<ProtectedResource> save(ProtectedResource resource) {
        return client.execute(
                        """
                        CREATE type::record('%s', $id) SET \
                        applicationId = $applicationId, tenantId = $tenantId, path = $path, method = $method, \
                        registeredAt = <datetime>$registeredAt;\
                        """.formatted(ProtectedResourceSchema.TABLE),
                        Map.of(
                                "id", resource.id().value().toString(),
                                "applicationId", resource.applicationId().value().toString(),
                                "tenantId", resource.tenantId().value(),
                                "path", resource.path().value(),
                                "method", resource.method().name(),
                                "registeredAt", resource.registeredAt().toString()))
                .thenReturn(resource);
    }

    @Override
    public Mono<Void> deleteById(ResourceId resourceId) {
        return client.execute(
                        "DELETE type::record('%s', $id);".formatted(ProtectedResourceSchema.TABLE),
                        Map.of("id", resourceId.value().toString()))
                .then();
    }

    @Override
    public Mono<ApplicationId> findApplicationIdById(ResourceId resourceId) {
        return client.execute(
                        "SELECT applicationId FROM type::record('%s', $id);".formatted(ProtectedResourceSchema.TABLE),
                        Map.of("id", resourceId.value().toString()))
                .flatMap(results -> {
                    JsonNode rows = results.get(0);
                    return rows.isEmpty() ? Mono.empty()
                            : Mono.just(ApplicationId.of(rows.get(0).path("applicationId").asString()));
                });
    }

    @Override
    public Mono<ResourceId> findIdByApplicationPathAndMethod(ApplicationId applicationId, ResourcePath path, HttpVerb method) {
        return client.execute("SELECT id FROM %s WHERE applicationId = $applicationId AND path = $path AND method = $method LIMIT 1;"
                        .formatted(ProtectedResourceSchema.TABLE), Map.of("applicationId", applicationId.value().toString(), "path", path.value(), "method", method.name()))
                .flatMap(results -> results.get(0).isEmpty() ? Mono.empty() : Mono.just(ResourceId.of(SurrealRecordId.idPart(results.get(0).get(0).path("id").asString()))));
    }

    private static ProtectedResource toDomain(JsonNode row) {
        ProtectedResourceEntity entity = new ProtectedResourceEntity(
                SurrealRecordId.idPart(row.path("id").asString()),
                row.path("applicationId").asString(),
                row.path("tenantId").asString(),
                row.path("path").asString(),
                row.path("method").asString(),
                Instant.parse(row.path("registeredAt").asString()));
        return ProtectedResourcePersistenceMapper.toDomain(entity);
    }
}
