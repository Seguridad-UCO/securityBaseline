package co.edu.uco.seguridad.pdp.roles.infrastructure.adapter.secondary.persistence.repository;

import co.edu.uco.seguridad.pdp.commons.model.PageWindow;
import co.edu.uco.seguridad.pdp.commons.model.ResultPage;
import co.edu.uco.seguridad.pdp.commons.model.RoleId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.roles.application.secondaryport.repository.RoleRepository;
import co.edu.uco.seguridad.pdp.roles.domain.Role;
import co.edu.uco.seguridad.pdp.roles.domain.RoleCriteria;
import co.edu.uco.seguridad.pdp.roles.domain.model.RoleName;
import co.edu.uco.seguridad.pdp.roles.domain.model.RoleScope;
import co.edu.uco.seguridad.pdp.roles.infrastructure.adapter.secondary.persistence.entity.RoleEntity;
import co.edu.uco.seguridad.pdp.roles.infrastructure.adapter.secondary.persistence.mapper.RolePersistenceMapper;
import co.edu.uco.seguridad.pdp.roles.infrastructure.adapter.secondary.persistence.schema.RoleSchema;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import co.edu.uco.seguridad.shared.persistence.surrealdb.SurrealDbClient;
import co.edu.uco.seguridad.shared.persistence.surrealdb.SurrealRecordId;
import reactor.core.publisher.Mono;
import tools.jackson.databind.JsonNode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/** Adaptador real sobre SurrealDB. Lee la fila en RoleEntity y delega en RolePersistenceMapper. */
public final class SurrealRoleRepository implements RoleRepository {

    private final SurrealDbClient client;

    public SurrealRoleRepository(SurrealDbClient client) {
        this.client = Objects.requireNonNull(client, RequiredArgumentMessages.SURREALDB_CLIENT);
    }

    @Override
    public Mono<Boolean> existsByNameInScope(RoleName name, RoleScope scope) {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("name", name.value());
        parameters.put("level", scope.level().name());
        String tenantIdExpr = bind(parameters, "tenantId", scope.tenantId().map(TenantId::value));
        String applicationIdExpr = bind(parameters, "applicationId",
                scope.applicationId().map(applicationId -> applicationId.value().toString()));

        return client.execute(
                        "SELECT id FROM %s WHERE name = $name AND level = $level AND tenantId = %s AND applicationId = %s LIMIT 1;"
                                .formatted(RoleSchema.TABLE, tenantIdExpr, applicationIdExpr),
                        parameters)
                .map(results -> !results.get(0).isEmpty());
    }

    @Override
    public Mono<Role> findByNameInScope(RoleName name, RoleScope scope) {
        throw new UnsupportedOperationException("pendiente: HU-015");
    }

    @Override
    public Mono<Role> findByIdForTenant(RoleId roleId, TenantId tenantId) {
        return client.execute(
                        "SELECT * FROM type::record('%s', $id) WHERE tenantId = $tenantId;".formatted(RoleSchema.TABLE),
                        Map.of("id", roleId.value().toString(), "tenantId", tenantId.value()))
                .flatMap(results -> {
                    JsonNode rows = results.get(0);
                    return rows.isEmpty() ? Mono.empty() : Mono.just(toDomain(rows.get(0)));
                });
    }

    @Override
    public Mono<Role> findById(RoleId roleId) {
        return client.execute(
                        "SELECT * FROM type::record('%s', $id);".formatted(RoleSchema.TABLE),
                        Map.of("id", roleId.value().toString()))
                .flatMap(results -> {
                    JsonNode rows = results.get(0);
                    return rows.isEmpty() ? Mono.empty() : Mono.just(toDomain(rows.get(0)));
                });
    }

    @Override
    public Mono<ResultPage<Role>> findBy(RoleCriteria criteria, PageWindow window) {
        String query = """
                SELECT * FROM %1$s WHERE tenantId = $tenantId OR level = 'GLOBAL' \
                ORDER BY registeredAt DESC LIMIT %2$d START %3$d;
                SELECT count() FROM %1$s WHERE tenantId = $tenantId OR level = 'GLOBAL' GROUP ALL;\
                """.formatted(RoleSchema.TABLE, window.limit(), window.offset());

        return client.execute(query, Map.of("tenantId", criteria.tenantId().value()))
                .map(results -> {
                    List<Role> content = results.get(0).valueStream().map(SurrealRoleRepository::toDomain).toList();
                    return ResultPage.of(content, totalOf(results.get(1)), window);
                });
    }

    private static long totalOf(JsonNode countResult) {
        if (countResult.isEmpty()) {
            return 0L;
        }
        return countResult.get(0).path("count").asLong(0L);
    }

    @Override
    public Mono<Role> save(Role role) {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("id", role.id().value().toString());
        parameters.put("level", role.scope().level().name());
        parameters.put("name", role.name().value());
        parameters.put("registeredAt", role.registeredAt().toString());
        String tenantIdExpr = bind(parameters, "tenantId", role.scope().tenantId().map(TenantId::value));
        String applicationIdExpr = bind(parameters, "applicationId",
                role.scope().applicationId().map(applicationId -> applicationId.value().toString()));
        String resourcesLiteral = role.resources().stream()
                .map(resourceId -> "'" + resourceId.value() + "'")
                .collect(Collectors.joining(",", "[", "]"));

        String query = """
                UPSERT type::record('%s', $id) SET \
                level = $level, name = $name, tenantId = %s, applicationId = %s, \
                resources = %s, registeredAt = <datetime>$registeredAt;\
                """.formatted(RoleSchema.TABLE, tenantIdExpr, applicationIdExpr, resourcesLiteral);

        return client.execute(query, parameters).thenReturn(role);
    }

    /** Une un componente opcional del alcance como parámetro ligado, o {@code NONE} si está ausente. */
    private static String bind(Map<String, String> parameters, String field, Optional<String> value) {
        return value.map(present -> {
            parameters.put(field, present);
            return "$" + field;
        }).orElse("NONE");
    }

    private static Role toDomain(JsonNode row) {
        RoleEntity entity = new RoleEntity(
                SurrealRecordId.idPart(row.path("id").asString()),
                row.path("name").asString(),
                row.path("level").asString(),
                row.path("tenantId").asString(null),
                row.path("applicationId").asString(null),
                row.path("resources").valueStream().map(JsonNode::asString).toList(),
                row.path("registeredAt").asString());
        return RolePersistenceMapper.toDomain(entity);
    }
}
