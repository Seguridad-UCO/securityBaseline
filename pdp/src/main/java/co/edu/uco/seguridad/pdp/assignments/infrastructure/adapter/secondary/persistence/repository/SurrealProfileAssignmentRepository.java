package co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.secondary.persistence.repository;

import co.edu.uco.seguridad.pdp.assignments.application.secondaryport.repository.ProfileAssignmentRepository;
import co.edu.uco.seguridad.pdp.assignments.domain.ProfileAssignment;
import co.edu.uco.seguridad.pdp.assignments.domain.ProfileAssignmentCriteria;
import co.edu.uco.seguridad.pdp.assignments.domain.model.ProfileAssignmentId;
import co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.secondary.persistence.entity.ProfileAssignmentEntity;
import co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.secondary.persistence.mapper.ProfileAssignmentPersistenceMapper;
import co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.secondary.persistence.schema.ProfileAssignmentSchema;
import co.edu.uco.seguridad.pdp.commons.model.*;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import co.edu.uco.seguridad.shared.persistence.surrealdb.SurrealDbClient;
import co.edu.uco.seguridad.shared.persistence.surrealdb.SurrealRecordId;
import reactor.core.publisher.Mono;
import tools.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Adaptador real sobre SurrealDB. Lee la fila en ProfileAssignmentEntity y delega en ProfileAssignmentPersistenceMapper.
 */
public final class SurrealProfileAssignmentRepository implements ProfileAssignmentRepository {

    private final SurrealDbClient client;

    public SurrealProfileAssignmentRepository(SurrealDbClient client) {
        this.client = Objects.requireNonNull(client, RequiredArgumentMessages.SURREALDB_CLIENT);
    }

    @Override
    public Mono<Boolean> existsActiveByUserApplicationProfile(UserId userId, ApplicationId applicationId,
                                                              ProfileId profileId, Instant now) {
        return client.execute(
                        """
                                SELECT id FROM %s WHERE userId = $userId AND applicationId = $applicationId \
                                AND profileId = $profileId AND validFrom <= <datetime>$now \
                                AND (validUntil = NONE OR validUntil > <datetime>$now) LIMIT 1;\
                                """.formatted(ProfileAssignmentSchema.TABLE),
                        Map.of("userId", userId.value().toString(), "applicationId", applicationId.value().toString(),
                                "profileId", profileId.value().toString(), "now", now.toString()))
                .map(results -> !results.get(0).isEmpty());
    }

    @Override
    public Mono<ProfileAssignment> findByIdForTenant(ProfileAssignmentId profileAssignmentId, TenantId tenantId) {
        return client.execute(
                        "SELECT * FROM type::record('%s', $id) WHERE tenantId = $tenantId;"
                                .formatted(ProfileAssignmentSchema.TABLE),
                        Map.of("id", profileAssignmentId.value().toString(), "tenantId", tenantId.value()))
                .flatMap(results -> {
                    JsonNode rows = results.get(0);
                    return rows.isEmpty() ? Mono.empty() : Mono.just(toDomain(rows.get(0)));
                });
    }

    @Override
    public Mono<ResultPage<ProfileAssignment>> findBy(ProfileAssignmentCriteria criteria, PageWindow window) {
        String query = """
                SELECT * FROM %1$s WHERE profileId = $profileId AND tenantId = $tenantId \
                ORDER BY validFrom DESC LIMIT %2$d START %3$d;
                SELECT count() FROM %1$s WHERE profileId = $profileId AND tenantId = $tenantId GROUP ALL;\
                """.formatted(ProfileAssignmentSchema.TABLE, window.limit(), window.offset());

        return client.execute(query, Map.of("profileId", criteria.profileId().value().toString(), "tenantId",
                        criteria.tenantId().value()))
                .map(results -> {
                    List<ProfileAssignment> content = results.get(0).valueStream()
                            .map(SurrealProfileAssignmentRepository::toDomain)
                            .toList();
                    return ResultPage.of(content, totalOf(results.get(1)), window);
                });
    }

    @Override
    public Mono<ResultPage<ProfileAssignment>> findPageByApplication(TenantId tenantId, ApplicationId applicationId,
                                                                     PageWindow window) {
        String query = """
                SELECT * FROM %1$s WHERE tenantId = $tenantId AND applicationId = $applicationId \
                ORDER BY validFrom DESC LIMIT %2$d START %3$d;
                SELECT count() FROM %1$s WHERE tenantId = $tenantId AND applicationId = $applicationId GROUP ALL;
                """.formatted(ProfileAssignmentSchema.TABLE, window.limit(), window.offset());
        return client.execute(query, Map.of("tenantId", tenantId.value(), "applicationId", applicationId.value().toString()))
                .map(results -> ResultPage.of(results.get(0).valueStream().map(SurrealProfileAssignmentRepository::toDomain).toList(), totalOf(results.get(1)), window));
    }

    @Override public Mono<Long> countByApplication(TenantId tenantId, ApplicationId applicationId) {
        return client.execute("SELECT count() FROM %s WHERE tenantId = $tenantId AND applicationId = $applicationId GROUP ALL;".formatted(ProfileAssignmentSchema.TABLE), Map.of("tenantId", tenantId.value(), "applicationId", applicationId.value().toString()))
                .map(r -> totalOf(r.get(0)));
    }

    @Override public Mono<ResultPage<ProfileAssignment>> findActivePageByProfileAndApplication(ProfileId profileId, TenantId tenantId, ApplicationId applicationId, Instant now, PageWindow window) {
        return findActive("profileId", profileId.value().toString(), tenantId, applicationId, now, window);
    }
    @Override public Mono<ResultPage<ProfileAssignment>> findActivePageByUserAndApplication(UserId userId, TenantId tenantId, ApplicationId applicationId, Instant now, PageWindow window) {
        return findActive("userId", userId.value().toString(), tenantId, applicationId, now, window);
    }
    private Mono<ResultPage<ProfileAssignment>> findActive(String field, String id, TenantId tenantId, ApplicationId applicationId, Instant now, PageWindow window) {
        String query = "SELECT * FROM %1$s WHERE %2$s = $id AND tenantId = $tenantId AND applicationId = $applicationId AND validFrom <= <datetime>$now AND (validUntil = NONE OR validUntil > <datetime>$now) ORDER BY validFrom DESC LIMIT %3$d START %4$d; SELECT count() FROM %1$s WHERE %2$s = $id AND tenantId = $tenantId AND applicationId = $applicationId AND validFrom <= <datetime>$now AND (validUntil = NONE OR validUntil > <datetime>$now) GROUP ALL;".formatted(ProfileAssignmentSchema.TABLE, field, window.limit(), window.offset());
        return client.execute(query, Map.of("id",id,"tenantId",tenantId.value(),"applicationId",applicationId.value().toString(),"now",now.toString())).map(rows -> ResultPage.of(rows.get(0).valueStream().map(SurrealProfileAssignmentRepository::toDomain).toList(), totalOf(rows.get(1)), window));
    }

    private static long totalOf(JsonNode countResult) {
        if (countResult.isEmpty()) {
            return 0L;
        }
        return countResult.get(0).path("count").asLong(0L);
    }

    @Override
    public Mono<Set<ProfileId>> findActiveProfileIdsFor(UserId userId, ApplicationId applicationId, Instant now) {
        return client.execute("""
                                SELECT profileId FROM %s WHERE userId = $userId AND applicationId = $applicationId \
                                AND validFrom <= <datetime>$now AND (validUntil = NONE OR validUntil > <datetime>$now);\
                                """.formatted(ProfileAssignmentSchema.TABLE),
                        Map.of("userId", userId.value().toString(), "applicationId", applicationId.value().toString(),
                                "now", now.toString()))
                .map(results -> results.get(0).valueStream().map(row -> ProfileId.of(row.path("profileId").asString()))
                        .collect(Collectors.toSet()));
    }

    @Override
    public Mono<ProfileAssignment> save(ProfileAssignment profileAssignment) {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("id", profileAssignment.id().value().toString());
        parameters.put("userId", profileAssignment.userId().value().toString());
        parameters.put("tenantId", profileAssignment.tenantId().value());
        parameters.put("applicationId", profileAssignment.applicationId().value().toString());
        parameters.put("profileId", profileAssignment.profileId().value().toString());
        parameters.put("validFrom", profileAssignment.validity().validFrom().toString());
        String validUntilExpr = profileAssignment.validity().validUntil().map(validUntil -> {
            parameters.put("validUntil", validUntil.toString());
            return "<datetime>$validUntil";
        }).orElse("NONE");
        String generatedIdsLiteral = profileAssignment.generatedAssignmentIds().stream()
                .map(assignmentId -> "'" + assignmentId.value() + "'")
                .collect(Collectors.joining(",", "[", "]"));

        String query = """
                UPSERT type::record('%s', $id) SET \
                userId = $userId, tenantId = $tenantId, applicationId = $applicationId, profileId = $profileId, \
                generatedAssignmentIds = %s, validFrom = <datetime>$validFrom, validUntil = %s;\
                """.formatted(ProfileAssignmentSchema.TABLE, generatedIdsLiteral, validUntilExpr);

        return client.execute(query, parameters).thenReturn(profileAssignment);
    }

    @Override
    public Mono<Boolean> existsActiveByProfileId(ProfileId profileId, Instant now) {
        return client.execute("""
                                SELECT id FROM %s WHERE profileId = $profileId AND validFrom <= <datetime>$now \
                                AND (validUntil = NONE OR validUntil > <datetime>$now) LIMIT 1;
                                """.formatted(ProfileAssignmentSchema.TABLE),
                        Map.of("profileId", profileId.value().toString(), "now", now.toString()))
                .map(results -> !results.get(0).isEmpty());
    }

    @Override
    public Mono<Boolean> existsByApplicationId(ApplicationId applicationId) {
        return client.execute("SELECT id FROM %s WHERE applicationId = $applicationId LIMIT 1;"
                                .formatted(ProfileAssignmentSchema.TABLE),
                        Map.of("applicationId", applicationId.value().toString()))
                .map(results -> !results.get(0).isEmpty());
    }

    private static ProfileAssignment toDomain(JsonNode row) {
        ProfileAssignmentEntity entity = new ProfileAssignmentEntity(
                SurrealRecordId.idPart(row.path("id").asString()),
                row.path("userId").asString(),
                row.path("tenantId").asString(),
                row.path("applicationId").asString(),
                row.path("profileId").asString(),
                row.path("generatedAssignmentIds").valueStream().map(JsonNode::asString).toList(),
                row.path("validFrom").asString(),
                row.path("validUntil").asString(null));
        return ProfileAssignmentPersistenceMapper.toDomain(entity);
    }
}
