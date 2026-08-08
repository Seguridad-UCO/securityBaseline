package co.edu.uco.seguridad.pdp.recursos.infrastructure.adapter.secondary.persistence.repository;

import co.edu.uco.seguridad.pdp.commons.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.PageWindow;
import co.edu.uco.seguridad.pdp.commons.ResourceId;
import co.edu.uco.seguridad.pdp.commons.ResultPage;
import co.edu.uco.seguridad.pdp.recursos.application.port.secondary.repository.ProtectedResourceRepository;
import co.edu.uco.seguridad.pdp.recursos.domain.ActionCode;
import co.edu.uco.seguridad.pdp.recursos.domain.ProtectedApplicationCriteria;
import co.edu.uco.seguridad.pdp.recursos.domain.ProtectedResource;
import co.edu.uco.seguridad.pdp.recursos.domain.ResourceCode;
import co.edu.uco.seguridad.pdp.recursos.infrastructure.adapter.secondary.persistence.entity.ProtectedResourceEntity;
import co.edu.uco.seguridad.pdp.recursos.infrastructure.adapter.secondary.persistence.mapper.ProtectedResourcePersistenceMapper;
import co.edu.uco.seguridad.shared.persistence.surrealdb.SurrealDbClient;
import co.edu.uco.seguridad.shared.persistence.surrealdb.SurrealRecordId;
import reactor.core.publisher.Mono;
import tools.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Adaptador secundario (driven) real sobre SurrealDB (ADR-0004). {@code findBy} ejecuta la
 * specification que recibe traduciéndola a {@code WHERE}: {@code matches} sigue viviendo en
 * {@link ProtectedApplicationCriteria} para el adaptador en memoria (y para las pruebas de dominio),
 * pero este adaptador decide su propia forma de ejecutarla — exactamente la promesa que el puerto
 * documenta.
 */
public final class SurrealProtectedResourceRepository implements ProtectedResourceRepository {

    private static final String TABLE = "protected_resource";

    private final SurrealDbClient client;

    public SurrealProtectedResourceRepository(SurrealDbClient client) {
        this.client = Objects.requireNonNull(client, "se requiere el cliente de SurrealDB");
    }

    @Override
    public Mono<Boolean> existsGrant(ApplicationId applicationId, ResourceCode resourceCode, ActionCode action) {
        return client.execute(
                        "SELECT id FROM %s WHERE applicationId = $applicationId AND resourceCode = $resourceCode AND action = $action LIMIT 1;"
                                .formatted(TABLE),
                        Map.of(
                                "applicationId", applicationId.value().toString(),
                                "resourceCode", resourceCode.value(),
                                "action", action.value()))
                .map(results -> !results.get(0).isEmpty());
    }

    @Override
    public Mono<ResultPage<ProtectedResource>> findBy(ProtectedApplicationCriteria criteria, PageWindow window) {
        StringBuilder where = new StringBuilder("tenantId = $tenantId");
        Map<String, String> params = new HashMap<>();
        params.put("tenantId", criteria.tenantId().value());
        criteria.nameContains().ifPresent(fragment -> {
            where.append(" AND applicationName CONTAINS $nameContains");
            params.put("nameContains", fragment);
        });
        criteria.resourceContains().ifPresent(fragment -> {
            where.append(" AND resourceCode CONTAINS $resourceContains");
            params.put("resourceContains", fragment);
        });
        params.put("limit", String.valueOf(window.limit()));
        params.put("offset", String.valueOf(window.offset()));

        String surql = """
                SELECT * FROM %s WHERE %s ORDER BY registeredAt ASC, id ASC LIMIT <int>$limit START <int>$offset;
                SELECT count() FROM %s WHERE %s GROUP ALL;
                """.formatted(TABLE, where, TABLE, where);

        return client.execute(surql, params)
                .map(results -> {
                    List<ProtectedResource> content = results.get(0).valueStream()
                            .map(SurrealProtectedResourceRepository::toDomain)
                            .toList();
                    long total = results.get(1).isEmpty() ? 0 : results.get(1).get(0).path("count").asLong();
                    return ResultPage.of(content, total, window);
                });
    }

    @Override
    public Mono<ProtectedResource> save(ProtectedResource resource) {
        return client.execute(
                        """
                        CREATE type::record('%s', $id) SET \
                        applicationId = $applicationId, tenantId = $tenantId, applicationName = $applicationName, \
                        resourceCode = $resourceCode, action = $action, registeredAt = <datetime>$registeredAt;\
                        """.formatted(TABLE),
                        Map.of(
                                "id", resource.id().value().toString(),
                                "applicationId", resource.applicationId().value().toString(),
                                "tenantId", resource.tenantId().value(),
                                "applicationName", resource.applicationName().value(),
                                "resourceCode", resource.code().value(),
                                "action", resource.action().value(),
                                "registeredAt", resource.registeredAt().toString()))
                .thenReturn(resource);
    }

    @Override
    public Mono<Void> deleteById(ResourceId resourceId) {
        return client.execute(
                        "DELETE type::record('%s', $id);".formatted(TABLE),
                        Map.of("id", resourceId.value().toString()))
                .then();
    }

    private static ProtectedResource toDomain(JsonNode row) {
        ProtectedResourceEntity entity = new ProtectedResourceEntity(
                SurrealRecordId.idPart(row.path("id").asString()),
                row.path("applicationId").asString(),
                row.path("tenantId").asString(),
                row.path("applicationName").asString(),
                row.path("resourceCode").asString(),
                row.path("action").asString(),
                Instant.parse(row.path("registeredAt").asString()));
        return ProtectedResourcePersistenceMapper.toDomain(entity);
    }
}
