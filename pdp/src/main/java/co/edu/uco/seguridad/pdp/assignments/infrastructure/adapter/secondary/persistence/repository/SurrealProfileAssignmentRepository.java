package co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.secondary.persistence.repository;

import co.edu.uco.seguridad.pdp.assignments.application.secondaryport.repository.ProfileAssignmentRepository;
import co.edu.uco.seguridad.pdp.assignments.domain.ProfileAssignment;
import co.edu.uco.seguridad.pdp.assignments.domain.model.ProfileAssignmentId;
import co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.secondary.persistence.entity.ProfileAssignmentEntity;
import co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.secondary.persistence.mapper.ProfileAssignmentPersistenceMapper;
import co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.secondary.persistence.schema.ProfileAssignmentSchema;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.ProfileId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.commons.model.UserId;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import co.edu.uco.seguridad.shared.persistence.surrealdb.SurrealDbClient;
import co.edu.uco.seguridad.shared.persistence.surrealdb.SurrealRecordId;
import reactor.core.publisher.Mono;
import tools.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.Set;

/** Adaptador real sobre SurrealDB. Lee la fila en ProfileAssignmentEntity y delega en ProfileAssignmentPersistenceMapper. */
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
