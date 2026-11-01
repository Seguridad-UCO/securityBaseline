package co.edu.uco.seguridad.pdp.authorization.application.serviceidentity.port;

import co.edu.uco.seguridad.pdp.authorization.domain.serviceidentity.ServiceAccessGrant;
import co.edu.uco.seguridad.pdp.authorization.domain.serviceidentity.ServiceIdentity;
import co.edu.uco.seguridad.pdp.authorization.domain.serviceidentity.ServiceIdentityId;
import co.edu.uco.seguridad.pdp.authorization.domain.serviceidentity.ServiceTechnicalRoleDefinition;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.ResourceId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.commons.model.PageWindow;
import co.edu.uco.seguridad.pdp.commons.model.ResultPage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.util.Set;
import java.util.UUID;

/** Persistencia de identidades técnicas y sus autorizaciones, separada de las asignaciones de usuarios. */
public interface ServiceIdentityRepository {
    Mono<ServiceIdentity> saveIdentity(ServiceIdentity identity);
    Mono<ServiceIdentity> findIdentity(TenantId tenantId, ServiceIdentityId id);
    Mono<ServiceIdentity> findActiveByClientId(String clientId);
    Flux<ServiceIdentity> findByConsumer(TenantId tenantId, ApplicationId applicationId);
    Mono<ServiceAccessGrant> saveGrant(ServiceAccessGrant grant);
    Mono<Boolean> hasPendingOrApprovedGrant(TenantId tenantId, ServiceIdentityId identityId, ResourceId resourceId);
    Mono<ServiceAccessGrant> findGrant(TenantId tenantId, UUID id);
    Flux<ServiceAccessGrant> findGrants(TenantId tenantId, ServiceIdentityId identityId);
    Flux<ServiceAccessGrant> findGrantsForTarget(TenantId tenantId, ApplicationId targetApplicationId);
    Mono<Set<ResourceId>> findApprovedResourceIds(TenantId tenantId, ServiceIdentityId identityId,
                                                   ApplicationId targetApplicationId);
    Mono<Set<String>> findApprovedTechnicalRoleCodes(TenantId tenantId, ServiceIdentityId identityId,
                                                     ApplicationId targetApplicationId);
    Mono<ServiceTechnicalRoleDefinition> saveTechnicalRole(ServiceTechnicalRoleDefinition role);
    Mono<ServiceTechnicalRoleDefinition> findTechnicalRole(TenantId tenantId, UUID id);
    Flux<ServiceTechnicalRoleDefinition> findTechnicalRoles(TenantId tenantId, ApplicationId applicationId);
    Mono<ResultPage<ServiceTechnicalRoleDefinition>> findTechnicalRolesPage(TenantId tenantId, ApplicationId applicationId,
                                                                              PageWindow window);
    Mono<Boolean> hasPendingOrApprovedTechnicalRoleGrant(TenantId tenantId, ServiceIdentityId identityId, UUID roleId);
    Mono<Void> replaceTechnicalRoleResources(TenantId tenantId, UUID roleId, Set<ResourceId> resourceIds);
    Mono<Set<ResourceId>> findTechnicalRoleResourceIds(TenantId tenantId, UUID roleId);
    Mono<ResultPage<ResourceId>> findTechnicalRoleResourceIdsPage(TenantId tenantId, UUID roleId, PageWindow window);
    Mono<Long> countTechnicalRoleResources(TenantId tenantId, UUID roleId);
    Mono<Void> addTechnicalRoleResource(TenantId tenantId, UUID roleId, ResourceId resourceId);
    Mono<Void> removeTechnicalRoleResource(TenantId tenantId, UUID roleId, ResourceId resourceId);
    Mono<Boolean> hasTechnicalRoleResource(TenantId tenantId, UUID roleId, ResourceId resourceId);
}
