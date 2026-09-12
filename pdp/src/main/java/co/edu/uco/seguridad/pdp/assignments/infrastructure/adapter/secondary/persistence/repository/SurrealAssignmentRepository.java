package co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.secondary.persistence.repository;

import co.edu.uco.seguridad.pdp.assignments.application.secondaryport.repository.AssignmentRepository;
import co.edu.uco.seguridad.pdp.assignments.domain.Assignment;
import co.edu.uco.seguridad.pdp.assignments.domain.AssignmentCriteria;
import co.edu.uco.seguridad.pdp.assignments.domain.model.AssignmentId;
import co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.secondary.persistence.entity.AssignmentEntity;
import co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.secondary.persistence.mapper.AssignmentPersistenceMapper;
import co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.secondary.persistence.schema.AssignmentSchema;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.PageWindow;
import co.edu.uco.seguridad.pdp.commons.model.ResultPage;
import co.edu.uco.seguridad.pdp.commons.model.RoleId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.commons.model.UserId;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import co.edu.uco.seguridad.shared.persistence.surrealdb.SurrealDbClient;
import co.edu.uco.seguridad.shared.persistence.surrealdb.SurrealRecordId;
import reactor.core.publisher.Mono;
import tools.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/** Adaptador real sobre SurrealDB. Lee la fila en AssignmentEntity y delega en AssignmentPersistenceMapper. */
public final class SurrealAssignmentRepository implements AssignmentRepository {

    private final SurrealDbClient client;

    public SurrealAssignmentRepository(SurrealDbClient client) {
        this.client = Objects.requireNonNull(client, RequiredArgumentMessages.SURREALDB_CLIENT);
    }

    @Override
    public Mono<Boolean> existsActiveByUserApplicationRole(UserId userId, ApplicationId applicationId, RoleId roleId,
            Instant now) {
        return client.execute(
                        """
                        SELECT id FROM %s WHERE userId = $userId AND applicationId = $applicationId AND roleId = $roleId \
                        AND validFrom <= <datetime>$now AND (validUntil = NONE OR validUntil > <datetime>$now) LIMIT 1;\
                        """.formatted(AssignmentSchema.TABLE),
                        Map.of("userId", userId.value().toString(), "applicationId", applicationId.value().toString(),
                                "roleId", roleId.value().toString(), "now", now.toString()))
                .map(results -> !results.get(0).isEmpty());
    }

    @Override
    public Mono<Assignment> findByIdForTenant(AssignmentId assignmentId, TenantId tenantId) {
        return client.execute(
                        "SELECT * FROM type::record('%s', $id) WHERE tenantId = $tenantId;".formatted(AssignmentSchema.TABLE),
                        Map.of("id", assignmentId.value().toString(), "tenantId", tenantId.value()))
                .flatMap(results -> {
                    JsonNode rows = results.get(0);
                    return rows.isEmpty() ? Mono.empty() : Mono.just(toDomain(rows.get(0)));
                });
    }

    @Override
    public Mono<ResultPage<Assignment>> findBy(AssignmentCriteria criteria, PageWindow window) {
        String query = """
                SELECT * FROM %1$s WHERE roleId = $roleId AND tenantId = $tenantId \
                ORDER BY validFrom DESC LIMIT %2$d START %3$d;
                SELECT count() FROM %1$s WHERE roleId = $roleId AND tenantId = $tenantId GROUP ALL;\
                """.formatted(AssignmentSchema.TABLE, window.limit(), window.offset());

        return client.execute(query,
                        Map.of("roleId", criteria.roleId().value().toString(), "tenantId", criteria.tenantId().value()))
                .map(results -> {
                    List<Assignment> content = results.get(0).valueStream()
                            .map(SurrealAssignmentRepository::toDomain)
                            .toList();
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
    public Mono<Set<RoleId>> findActiveRoleIdsFor(UserId userId, ApplicationId applicationId, Instant now) {
        return client.execute(
                        """
                        SELECT roleId FROM %s WHERE userId = $userId AND applicationId = $applicationId \
                        AND validFrom <= <datetime>$now AND (validUntil = NONE OR validUntil > <datetime>$now);\
                        """.formatted(AssignmentSchema.TABLE),
                        Map.of("userId", userId.value().toString(), "applicationId", applicationId.value().toString(),
                                "now", now.toString()))
                .map(results -> results.get(0).valueStream()
                        .map(row -> RoleId.of(row.path("roleId").asString()))
                        .collect(Collectors.toSet()));
    }

    @Override
    public Mono<Assignment> save(Assignment assignment) {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("id", assignment.id().value().toString());
        parameters.put("userId", assignment.userId().value().toString());
        parameters.put("tenantId", assignment.tenantId().value());
        parameters.put("applicationId", assignment.applicationId().value().toString());
        parameters.put("roleId", assignment.roleId().value().toString());
        parameters.put("validFrom", assignment.validity().validFrom().toString());
        String validUntilExpr = assignment.validity().validUntil().map(validUntil -> {
            parameters.put("validUntil", validUntil.toString());
            return "<datetime>$validUntil";
        }).orElse("NONE");

        String query = """
                UPSERT type::record('%s', $id) SET \
                userId = $userId, tenantId = $tenantId, applicationId = $applicationId, roleId = $roleId, \
                validFrom = <datetime>$validFrom, validUntil = %s;\
                """.formatted(AssignmentSchema.TABLE, validUntilExpr);

        return client.execute(query, parameters).thenReturn(assignment);
    }

    private static Assignment toDomain(JsonNode row) {
        AssignmentEntity entity = new AssignmentEntity(
                SurrealRecordId.idPart(row.path("id").asString()),
                row.path("userId").asString(),
                row.path("tenantId").asString(),
                row.path("applicationId").asString(),
                row.path("roleId").asString(),
                row.path("validFrom").asString(),
                row.path("validUntil").asString(null));
        return AssignmentPersistenceMapper.toDomain(entity);
    }
}
