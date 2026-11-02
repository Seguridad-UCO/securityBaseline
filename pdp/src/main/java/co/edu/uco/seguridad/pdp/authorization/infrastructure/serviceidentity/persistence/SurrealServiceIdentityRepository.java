package co.edu.uco.seguridad.pdp.authorization.infrastructure.serviceidentity.persistence;

import co.edu.uco.seguridad.pdp.authorization.application.serviceidentity.port.ServiceIdentityRepository;
import co.edu.uco.seguridad.pdp.authorization.domain.serviceidentity.*;
import co.edu.uco.seguridad.pdp.commons.model.*;
import co.edu.uco.seguridad.shared.persistence.surrealdb.SurrealDbClient;
import co.edu.uco.seguridad.shared.persistence.surrealdb.SurrealRecordId;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tools.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Adaptador SurrealQL del catálogo M2M. Los grants se conservan para auditoría aun tras revocar.
 */
public final class SurrealServiceIdentityRepository implements ServiceIdentityRepository {
    public static final String IDENTITY_TABLE = "service_identity";
    public static final String GRANT_TABLE = "service_access_grant";
    public static final String TECHNICAL_ROLE_TABLE = "service_technical_role";
    public static final String TECHNICAL_ROLE_RESOURCE_TABLE = "service_technical_role_resource";
    private final SurrealDbClient client;

    public SurrealServiceIdentityRepository(SurrealDbClient client) {
        this.client = Objects.requireNonNull(client);
    }

    @Override
    public Mono<ServiceIdentity> saveIdentity(ServiceIdentity identity) {
        return client.execute("UPSERT type::record('service_identity', $id) SET tenantId=$tenantId, consumerApplicationId=$applicationId, name=$name, clientId=$clientId, status=$status, registeredAt=<datetime>$registeredAt;",
                Map.of("id", identity.id().value().toString(), "tenantId", identity.tenantId().value(), "applicationId", identity.consumerApplicationId().value().toString(), "name", identity.name(), "clientId", identity.clientId(), "status", identity.status().name(), "registeredAt", identity.registeredAt().toString())).thenReturn(identity);
    }

    @Override
    public Mono<ServiceIdentity> findIdentity(TenantId tenantId, ServiceIdentityId id) {
        return client.execute("SELECT * FROM type::record('service_identity', $id) WHERE tenantId=$tenantId;", Map.of("id", id.value().toString(), "tenantId", tenantId.value())).flatMap(r -> r.get(0).isEmpty() ? Mono.empty() : Mono.just(identity(r.get(0).get(0))));
    }

    @Override
    public Mono<ServiceIdentity> findActiveByClientId(String clientId) {
        return client.execute("SELECT * FROM service_identity WHERE clientId=$clientId AND status='ACTIVE' LIMIT 1;", Map.of("clientId", clientId)).flatMap(r -> r.get(0).isEmpty() ? Mono.empty() : Mono.just(identity(r.get(0).get(0))));
    }

    @Override
    public Flux<ServiceIdentity> findByConsumer(TenantId tenantId, ApplicationId applicationId) {
        return client.execute("SELECT * FROM service_identity WHERE tenantId=$tenantId AND consumerApplicationId=$applicationId ORDER BY registeredAt DESC;", Map.of("tenantId", tenantId.value(), "applicationId", applicationId.value().toString())).flatMapMany(r -> Flux.fromIterable(r.get(0).valueStream().toList())).map(SurrealServiceIdentityRepository::identity);
    }

    @Override
    public Mono<ServiceAccessGrant> saveGrant(ServiceAccessGrant grant) {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("id", grant.id().toString());
        parameters.put("tenantId", grant.tenantId().value());
        parameters.put("identityId", grant.serviceIdentityId().value().toString());
        parameters.put("targetApplicationId", grant.targetApplicationId().value().toString());
        parameters.put("technicalRole", grant.technicalRole());
        parameters.put("status", grant.status().name());
        parameters.put("requestedAt", grant.requestedAt().toString());
        String resource = grant.resourceId() == null ? "NONE" : "$resourceId";
        if (grant.resourceId() != null) parameters.put("resourceId", grant.resourceId().value().toString());
        String roleId = grant.technicalRoleId() == null ? "NONE" : "$technicalRoleId";
        if (grant.technicalRoleId() != null) parameters.put("technicalRoleId", grant.technicalRoleId().toString());
        String decided = grant.decidedAt() == null ? "NONE" : "<datetime>$decidedAt";
        if (grant.decidedAt() != null) parameters.put("decidedAt", grant.decidedAt().toString());
        return client.execute("UPSERT type::record('service_access_grant', $id) SET tenantId=$tenantId, serviceIdentityId=$identityId, targetApplicationId=$targetApplicationId, resourceId=" + resource + ", technicalRoleId=" + roleId + ", technicalRole=$technicalRole, status=$status, requestedAt=<datetime>$requestedAt, decidedAt=" + decided + ";", parameters).thenReturn(grant);
    }

    @Override
    public Mono<Boolean> hasPendingOrApprovedGrant(TenantId tenantId, ServiceIdentityId identityId, ResourceId resourceId) {
        return client.execute("SELECT id FROM service_access_grant WHERE tenantId=$tenantId AND serviceIdentityId=$identityId AND resourceId=$resourceId AND status INSIDE ['PENDING', 'APPROVED'] LIMIT 1;", Map.of("tenantId", tenantId.value(), "identityId", identityId.value().toString(), "resourceId", resourceId.value().toString()))
                .map(result -> !result.get(0).isEmpty());
    }

    @Override
    public Mono<ServiceAccessGrant> findGrant(TenantId tenantId, UUID id) {
        return client.execute("SELECT * FROM type::record('service_access_grant',$id) WHERE tenantId=$tenantId;", Map.of("id", id.toString(), "tenantId", tenantId.value())).flatMap(r -> r.get(0).isEmpty() ? Mono.empty() : Mono.just(grant(r.get(0).get(0))));
    }

    @Override
    public Flux<ServiceAccessGrant> findGrants(TenantId tenantId, ServiceIdentityId identityId) {
        return client.execute("SELECT * FROM service_access_grant WHERE tenantId=$tenantId AND serviceIdentityId=$id ORDER BY requestedAt DESC;", Map.of("tenantId", tenantId.value(), "id", identityId.value().toString())).flatMapMany(r -> Flux.fromIterable(r.get(0).valueStream().toList())).map(SurrealServiceIdentityRepository::grant);
    }

    @Override
    public Flux<ServiceAccessGrant> findGrantsForTarget(TenantId tenantId, ApplicationId targetApplicationId) {
        return client.execute("SELECT * FROM service_access_grant WHERE tenantId=$tenantId AND targetApplicationId=$applicationId ORDER BY requestedAt DESC;", Map.of("tenantId", tenantId.value(), "applicationId", targetApplicationId.value().toString()))
                .flatMapMany(r -> Flux.fromIterable(r.get(0).valueStream().toList())).map(SurrealServiceIdentityRepository::grant);
    }

    @Override
    public Mono<Set<ResourceId>> findApprovedResourceIds(TenantId tenantId, ServiceIdentityId identityId, ApplicationId targetApplicationId) {
        Map<String, String> params = Map.of("tenantId", tenantId.value(), "id", identityId.value().toString(), "applicationId", targetApplicationId.value().toString());
        Mono<Set<ResourceId>> direct = client.execute("SELECT resourceId FROM service_access_grant WHERE tenantId=$tenantId AND serviceIdentityId=$id AND targetApplicationId=$applicationId AND technicalRole='SERVICE_CONSUMER' AND status='APPROVED';", params).map(r -> r.get(0).valueStream().map(row -> ResourceId.of(row.path("resourceId").asString())).collect(Collectors.toSet()));
        Mono<Set<ResourceId>> fromRoles = client.execute("SELECT technicalRoleId FROM service_access_grant WHERE tenantId=$tenantId AND serviceIdentityId=$id AND targetApplicationId=$applicationId AND technicalRoleId IS NOT NONE AND status='APPROVED';", params)
                .flatMapMany(r -> Flux.fromIterable(r.get(0).valueStream().toList()))
                .flatMap(row -> findTechnicalRoleResourceIds(tenantId, UUID.fromString(row.path("technicalRoleId").asString())))
                .flatMapIterable(resources -> resources).collect(Collectors.toSet());
        return Mono.zip(direct, fromRoles).map(pair -> {
            var all = new java.util.HashSet<ResourceId>(pair.getT1());
            all.addAll(pair.getT2());
            return Set.copyOf(all);
        });
    }

    @Override
    public Mono<Set<String>> findApprovedTechnicalRoleCodes(TenantId tenantId, ServiceIdentityId identityId, ApplicationId targetApplicationId) {
        return client.execute("SELECT technicalRole FROM service_access_grant WHERE tenantId=$tenantId AND serviceIdentityId=$id AND targetApplicationId=$applicationId AND status='APPROVED';", Map.of("tenantId", tenantId.value(), "id", identityId.value().toString(), "applicationId", targetApplicationId.value().toString()))
                .map(result -> result.get(0).valueStream().map(row -> row.path("technicalRole").asString()).collect(Collectors.toSet()));
    }

    @Override
    public Mono<ServiceTechnicalRoleDefinition> saveTechnicalRole(ServiceTechnicalRoleDefinition role) {
        return client.execute("UPSERT type::record('service_technical_role',$id) SET tenantId=$tenantId, applicationId=$applicationId, code=$code, name=$name, createdAt=<datetime>$createdAt;", Map.of("id", role.id().toString(), "tenantId", role.tenantId().value(), "applicationId", role.applicationId().value().toString(), "code", role.code(), "name", role.name(), "createdAt", role.createdAt().toString())).thenReturn(role);
    }

    @Override
    public Mono<ServiceTechnicalRoleDefinition> findTechnicalRole(TenantId tenantId, UUID id) {
        return client.execute("SELECT * FROM type::record('service_technical_role',$id) WHERE tenantId=$tenantId;", Map.of("id", id.toString(), "tenantId", tenantId.value())).flatMap(r -> r.get(0).isEmpty() ? Mono.empty() : Mono.just(technicalRole(r.get(0).get(0))));
    }

    @Override
    public Flux<ServiceTechnicalRoleDefinition> findTechnicalRoles(TenantId tenantId, ApplicationId applicationId) {
        return client.execute("SELECT * FROM service_technical_role WHERE tenantId=$tenantId AND applicationId=$applicationId ORDER BY createdAt DESC;", Map.of("tenantId", tenantId.value(), "applicationId", applicationId.value().toString())).flatMapMany(r -> Flux.fromIterable(r.get(0).valueStream().toList())).map(SurrealServiceIdentityRepository::technicalRole);
    }

    @Override
    public Mono<ResultPage<ServiceTechnicalRoleDefinition>> findTechnicalRolesPage(TenantId tenantId, ApplicationId applicationId, PageWindow window) {
        Map<String, String> params = Map.of("tenantId", tenantId.value(), "applicationId", applicationId.value().toString());
        Mono<Long> count = client.execute("SELECT count() AS total FROM service_technical_role WHERE tenantId=$tenantId AND applicationId=$applicationId GROUP ALL;", params).map(result -> result.get(0).isEmpty() ? 0L : result.get(0).get(0).path("total").asLong());
        Mono<java.util.List<ServiceTechnicalRoleDefinition>> content = client.execute("SELECT * FROM service_technical_role WHERE tenantId=$tenantId AND applicationId=$applicationId ORDER BY createdAt DESC LIMIT " + window.limit() + " START " + window.offset() + ";", params).map(result -> result.get(0).valueStream().map(SurrealServiceIdentityRepository::technicalRole).toList());
        return Mono.zip(content, count).map(values -> ResultPage.of(values.getT1(), values.getT2(), window));
    }

    @Override
    public Mono<Boolean> hasPendingOrApprovedTechnicalRoleGrant(TenantId tenantId, ServiceIdentityId identityId, UUID roleId) {
        return client.execute("SELECT id FROM service_access_grant WHERE tenantId=$tenantId AND serviceIdentityId=$identityId AND technicalRoleId=$roleId AND status INSIDE ['PENDING','APPROVED'] LIMIT 1;", Map.of("tenantId", tenantId.value(), "identityId", identityId.value().toString(), "roleId", roleId.toString())).map(r -> !r.get(0).isEmpty());
    }

    @Override
    public Mono<Void> replaceTechnicalRoleResources(TenantId tenantId, UUID roleId, Set<ResourceId> resourceIds) {
        return client.execute("DELETE service_technical_role_resource WHERE tenantId=$tenantId AND technicalRoleId=$roleId;", Map.of("tenantId", tenantId.value(), "roleId", roleId.toString())).thenMany(Flux.fromIterable(resourceIds).flatMap(resourceId -> client.execute("CREATE service_technical_role_resource SET tenantId=$tenantId, technicalRoleId=$roleId, resourceId=$resourceId;", Map.of("tenantId", tenantId.value(), "roleId", roleId.toString(), "resourceId", resourceId.value().toString())))).then();
    }

    @Override
    public Mono<Set<ResourceId>> findTechnicalRoleResourceIds(TenantId tenantId, UUID roleId) {
        return client.execute("SELECT resourceId FROM service_technical_role_resource WHERE tenantId=$tenantId AND technicalRoleId=$roleId;", Map.of("tenantId", tenantId.value(), "roleId", roleId.toString())).map(r -> r.get(0).valueStream().map(row -> ResourceId.of(row.path("resourceId").asString())).collect(Collectors.toSet()));
    }

    @Override
    public Mono<ResultPage<ResourceId>> findTechnicalRoleResourceIdsPage(TenantId tenantId, UUID roleId, PageWindow window) {
        return Mono.zip(client.execute("SELECT resourceId FROM service_technical_role_resource WHERE tenantId=$tenantId AND technicalRoleId=$roleId ORDER BY resourceId LIMIT " + window.limit() + " START " + window.offset() + ";", Map.of("tenantId", tenantId.value(), "roleId", roleId.toString())).map(result -> result.get(0).valueStream().map(row -> ResourceId.of(row.path("resourceId").asString())).toList()), countTechnicalRoleResources(tenantId, roleId)).map(values -> ResultPage.of(values.getT1(), values.getT2(), window));
    }

    @Override
    public Mono<Long> countTechnicalRoleResources(TenantId tenantId, UUID roleId) {
        return client.execute("SELECT count() AS total FROM service_technical_role_resource WHERE tenantId=$tenantId AND technicalRoleId=$roleId GROUP ALL;", Map.of("tenantId", tenantId.value(), "roleId", roleId.toString())).map(result -> result.get(0).isEmpty() ? 0L : result.get(0).get(0).path("total").asLong());
    }

    @Override
    public Mono<Void> addTechnicalRoleResource(TenantId tenantId, UUID roleId, ResourceId resourceId) {
        return client.execute("CREATE service_technical_role_resource SET tenantId=$tenantId, technicalRoleId=$roleId, resourceId=$resourceId;", Map.of("tenantId", tenantId.value(), "roleId", roleId.toString(), "resourceId", resourceId.value().toString())).then();
    }

    @Override
    public Mono<Void> removeTechnicalRoleResource(TenantId tenantId, UUID roleId, ResourceId resourceId) {
        return client.execute("DELETE service_technical_role_resource WHERE tenantId=$tenantId AND technicalRoleId=$roleId AND resourceId=$resourceId;", Map.of("tenantId", tenantId.value(), "roleId", roleId.toString(), "resourceId", resourceId.value().toString())).then();
    }

    @Override
    public Mono<Boolean> hasTechnicalRoleResource(TenantId tenantId, UUID roleId, ResourceId resourceId) {
        return client.execute("SELECT id FROM service_technical_role_resource WHERE tenantId=$tenantId AND technicalRoleId=$roleId AND resourceId=$resourceId LIMIT 1;", Map.of("tenantId", tenantId.value(), "roleId", roleId.toString(), "resourceId", resourceId.value().toString())).map(result -> !result.get(0).isEmpty());
    }

    private static ServiceIdentity identity(JsonNode row) {
        return new ServiceIdentity(ServiceIdentityId.of(SurrealRecordId.idPart(row.path("id").asString())), new TenantId(row.path("tenantId").asString()), ApplicationId.of(row.path("consumerApplicationId").asString()), row.path("name").asString(), row.path("clientId").asString(), ServiceIdentityStatus.valueOf(row.path("status").asString()), Instant.parse(row.path("registeredAt").asString()));
    }

    private static ServiceAccessGrant grant(JsonNode row) {
        String decided = row.path("decidedAt").asString(null);
        String resource = row.path("resourceId").asString(null);
        String technicalRoleId = row.path("technicalRoleId").asString(null);
        return new ServiceAccessGrant(UUID.fromString(SurrealRecordId.idPart(row.path("id").asString())), new TenantId(row.path("tenantId").asString()), ServiceIdentityId.of(row.path("serviceIdentityId").asString()), ApplicationId.of(row.path("targetApplicationId").asString()), resource == null ? null : ResourceId.of(resource), technicalRoleId == null ? null : UUID.fromString(technicalRoleId), row.path("technicalRole").asString(), ServiceAccessStatus.valueOf(row.path("status").asString()), Instant.parse(row.path("requestedAt").asString()), decided == null ? null : Instant.parse(decided));
    }

    private static ServiceTechnicalRoleDefinition technicalRole(JsonNode row) {
        return new ServiceTechnicalRoleDefinition(UUID.fromString(SurrealRecordId.idPart(row.path("id").asString())), new TenantId(row.path("tenantId").asString()), ApplicationId.of(row.path("applicationId").asString()), row.path("code").asString(), row.path("name").asString(), Instant.parse(row.path("createdAt").asString()));
    }
}
