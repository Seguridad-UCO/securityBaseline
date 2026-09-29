package co.edu.uco.seguridad.pdp.profiles.infrastructure.adapter.secondary.persistence.repository;

import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.PageWindow;
import co.edu.uco.seguridad.pdp.commons.model.ProfileId;
import co.edu.uco.seguridad.pdp.commons.model.ResultPage;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.profiles.application.secondaryport.repository.ProfileRepository;
import co.edu.uco.seguridad.pdp.profiles.domain.Profile;
import co.edu.uco.seguridad.pdp.profiles.domain.ProfileCriteria;
import co.edu.uco.seguridad.pdp.profiles.domain.model.ProfileName;
import co.edu.uco.seguridad.pdp.profiles.infrastructure.adapter.secondary.persistence.entity.ProfileEntity;
import co.edu.uco.seguridad.pdp.profiles.infrastructure.adapter.secondary.persistence.mapper.ProfilePersistenceMapper;
import co.edu.uco.seguridad.pdp.profiles.infrastructure.adapter.secondary.persistence.schema.ProfileSchema;
import co.edu.uco.seguridad.pdp.roles.domain.model.RoleScope;
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

/** Adaptador real sobre SurrealDB. Lee la fila en ProfileEntity y delega en ProfilePersistenceMapper. Espejo de SurrealRoleRepository. */
public final class SurrealProfileRepository implements ProfileRepository {

    private final SurrealDbClient client;

    public SurrealProfileRepository(SurrealDbClient client) {
        this.client = Objects.requireNonNull(client, RequiredArgumentMessages.SURREALDB_CLIENT);
    }

    @Override
    public Mono<Boolean> existsByNameInScope(ProfileName name, RoleScope scope) {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("name", name.value());
        parameters.put("level", scope.level().name());
        String tenantIdExpr = bind(parameters, "tenantId", scope.tenantId().map(TenantId::value));
        String applicationIdExpr = bind(parameters, "applicationId",
                scope.applicationId().map(applicationId -> applicationId.value().toString()));

        return client.execute(
                        "SELECT id FROM %s WHERE name = $name AND level = $level AND tenantId = %s AND applicationId = %s LIMIT 1;"
                                .formatted(ProfileSchema.TABLE, tenantIdExpr, applicationIdExpr),
                        parameters)
                .map(results -> !results.get(0).isEmpty());
    }

    @Override
    public Mono<Profile> findByIdForTenant(ProfileId profileId, TenantId tenantId) {
        return client.execute(
                        "SELECT * FROM type::record('%s', $id) WHERE tenantId = $tenantId;".formatted(ProfileSchema.TABLE),
                        Map.of("id", profileId.value().toString(), "tenantId", tenantId.value()))
                .flatMap(results -> {
                    JsonNode rows = results.get(0);
                    return rows.isEmpty() ? Mono.empty() : Mono.just(toDomain(rows.get(0)));
                });
    }

    @Override
    public Mono<Profile> findById(ProfileId profileId) {
        return client.execute("SELECT * FROM type::record('%s', $id);".formatted(ProfileSchema.TABLE),
                        Map.of("id", profileId.value().toString()))
                .flatMap(results -> results.get(0).isEmpty() ? Mono.empty() : Mono.just(toDomain(results.get(0).get(0))));
    }

    @Override
    public Mono<ResultPage<Profile>> findBy(ProfileCriteria criteria, PageWindow window) {
        String query = """
                SELECT * FROM %1$s WHERE tenantId = $tenantId OR level = 'GLOBAL' \
                ORDER BY registeredAt DESC LIMIT %2$d START %3$d;
                SELECT count() FROM %1$s WHERE tenantId = $tenantId OR level = 'GLOBAL' GROUP ALL;\
                """.formatted(ProfileSchema.TABLE, window.limit(), window.offset());

        return client.execute(query, Map.of("tenantId", criteria.tenantId().value()))
                .map(results -> {
                    List<Profile> content = results.get(0).valueStream().map(SurrealProfileRepository::toDomain).toList();
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
    public Mono<Profile> save(Profile profile) {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("id", profile.id().value().toString());
        parameters.put("level", profile.scope().level().name());
        parameters.put("name", profile.name().value());
        parameters.put("registeredAt", profile.registeredAt().toString());
        String tenantIdExpr = bind(parameters, "tenantId", profile.scope().tenantId().map(TenantId::value));
        String applicationIdExpr = bind(parameters, "applicationId",
                profile.scope().applicationId().map(applicationId -> applicationId.value().toString()));
        String rolesLiteral = profile.roles().stream()
                .map(roleId -> "'" + roleId.value() + "'")
                .collect(Collectors.joining(",", "[", "]"));

        String query = """
                UPSERT type::record('%s', $id) SET \
                level = $level, name = $name, tenantId = %s, applicationId = %s, \
                roles = %s, registeredAt = <datetime>$registeredAt;\
                """.formatted(ProfileSchema.TABLE, tenantIdExpr, applicationIdExpr, rolesLiteral);

        return client.execute(query, parameters).thenReturn(profile);
    }

    @Override
    public Mono<Void> deleteById(ProfileId profileId) {
        return client.execute("DELETE type::record('%s', $id);".formatted(ProfileSchema.TABLE),
                        Map.of("id", profileId.value().toString()))
                .then();
    }

    @Override
    public Mono<Boolean> existsByRoleId(co.edu.uco.seguridad.pdp.commons.model.RoleId roleId) {
        return client.execute("SELECT id FROM %s WHERE $roleId INSIDE roles LIMIT 1;".formatted(ProfileSchema.TABLE),
                        Map.of("roleId", roleId.value().toString()))
                .map(results -> !results.get(0).isEmpty());
    }

    @Override
    public Mono<Boolean> existsByApplicationId(ApplicationId applicationId) {
        return client.execute("SELECT id FROM %s WHERE applicationId = $applicationId LIMIT 1;".formatted(ProfileSchema.TABLE),
                        Map.of("applicationId", applicationId.value().toString()))
                .map(results -> !results.get(0).isEmpty());
    }

    /**
     * Une un componente opcional del alcance como parámetro ligado, con cadena vacía como centinela
     * de ausencia. {@code NONE} no sirve: el índice único {@code profile_scope_name} no indexa ni
     * compara correctamente un campo ausente contra {@code NONE} (confirmado contra SurrealDB:
     * {@code WHERE applicationId = NONE} no encuentra la fila que acaba de insertarse con
     * {@code applicationId = NONE}, y el índice UNIQUE no rechaza un segundo registro idéntico). Una
     * cadena vacía es un valor real que el índice sí puede indexar y comparar.
     */
    private static String bind(Map<String, String> parameters, String field, Optional<String> value) {
        parameters.put(field, value.orElse(""));
        return "$" + field;
    }

    private static Profile toDomain(JsonNode row) {
        ProfileEntity entity = new ProfileEntity(
                SurrealRecordId.idPart(row.path("id").asString()),
                row.path("name").asString(),
                row.path("level").asString(),
                row.path("tenantId").asString(null),
                row.path("applicationId").asString(null),
                row.path("roles").valueStream().map(JsonNode::asString).toList(),
                row.path("registeredAt").asString());
        return ProfilePersistenceMapper.toDomain(entity);
    }
}
