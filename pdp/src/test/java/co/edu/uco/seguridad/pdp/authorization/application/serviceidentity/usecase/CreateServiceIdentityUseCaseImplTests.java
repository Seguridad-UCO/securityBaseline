package co.edu.uco.seguridad.pdp.authorization.application.serviceidentity.usecase;

import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AdministrationRequest;
import co.edu.uco.seguridad.pdp.authorization.application.rule.validator.PrincipalMustBeApplicationAdministratorValidator;
import co.edu.uco.seguridad.pdp.authorization.application.serviceidentity.port.ServiceClientProvisioner;
import co.edu.uco.seguridad.pdp.authorization.application.serviceidentity.port.ServiceIdentityRepository;
import co.edu.uco.seguridad.pdp.authorization.application.serviceidentity.request.CreateServiceIdentityRequest;
import co.edu.uco.seguridad.pdp.authorization.application.serviceidentity.service.ServiceIdentityAdministrationAuditor;
import co.edu.uco.seguridad.pdp.authorization.domain.serviceidentity.ServiceAccessGrant;
import co.edu.uco.seguridad.pdp.authorization.domain.serviceidentity.ServiceIdentity;
import co.edu.uco.seguridad.pdp.authorization.domain.serviceidentity.ServiceIdentityId;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.ResourceId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.commons.model.UserId;
import co.edu.uco.seguridad.shared.audit.TestAdministrationAuditRepositories;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class CreateServiceIdentityUseCaseImplTests {
    private static final TenantId TENANT = new TenantId("tenant-m2m");
    private static final ApplicationId APPLICATION = new ApplicationId(UUID.fromString("00000000-0000-0000-0000-000000000101"));
    private static final AdministrationRequest ADMINISTRATION = new AdministrationRequest(TENANT, APPLICATION,
            new UserId(UUID.fromString("00000000-0000-0000-0000-000000000102")), "admin", Set.of());
    private static final Instant NOW = Instant.parse("2026-11-01T00:00:00Z");

    @Test
    void compensates_the_keycloak_client_when_the_identity_cannot_be_persisted() {
        AtomicBoolean deprovisioned = new AtomicBoolean();
        ServiceClientProvisioner clients = new ServiceClientProvisioner() {
            @Override
            public Mono<ProvisionedServiceClient> provision(String clientId) {
                return Mono.just(new ProvisionedServiceClient(clientId, "secret"));
            }

            @Override
            public Mono<Void> deprovision(String clientId) {
                deprovisioned.set(true);
                return Mono.empty();
            }
        };
        CreateServiceIdentityUseCaseImpl useCase = new CreateServiceIdentityUseCaseImpl(allows(), clients,
                failingRepository(), () -> UUID.fromString("00000000-0000-0000-0000-000000000103"), () -> NOW,
                new ServiceIdentityAdministrationAuditor(TestAdministrationAuditRepositories.capturing(new java.util.ArrayList<>()),
                        () -> UUID.fromString("00000000-0000-0000-0000-000000000104"), () -> NOW));

        StepVerifier.create(useCase.execute(new CreateServiceIdentityRequest(ADMINISTRATION, "notes-projects")))
                .expectErrorMessage("surreal unavailable")
                .verify();

        assertThat(deprovisioned).isTrue();
    }

    private static PrincipalMustBeApplicationAdministratorValidator allows() {
        return ignored -> Mono.empty();
    }

    private static ServiceIdentityRepository failingRepository() {
        return new ServiceIdentityRepository() {
            @Override
            public Mono<ServiceIdentity> saveIdentity(ServiceIdentity identity) {
                return Mono.error(new IllegalStateException("surreal unavailable"));
            }

            @Override
            public Mono<ServiceIdentity> findIdentity(TenantId tenantId, ServiceIdentityId id) {
                return Mono.empty();
            }

            @Override
            public Mono<ServiceIdentity> findActiveByClientId(String clientId) {
                return Mono.empty();
            }

            @Override
            public Flux<ServiceIdentity> findByConsumer(TenantId tenantId, ApplicationId applicationId) {
                return Flux.empty();
            }

            @Override
            public Mono<ServiceAccessGrant> saveGrant(ServiceAccessGrant grant) {
                return Mono.empty();
            }

            @Override
            public Mono<Boolean> hasPendingOrApprovedGrant(TenantId tenantId, ServiceIdentityId identityId, ResourceId resourceId) {
                return Mono.just(false);
            }

            @Override
            public Mono<ServiceAccessGrant> findGrant(TenantId tenantId, UUID id) {
                return Mono.empty();
            }

            @Override
            public Flux<ServiceAccessGrant> findGrants(TenantId tenantId, ServiceIdentityId identityId) {
                return Flux.empty();
            }

            @Override
            public Flux<ServiceAccessGrant> findGrantsForTarget(TenantId tenantId, ApplicationId targetApplicationId) {
                return Flux.empty();
            }

            @Override
            public Mono<Set<ResourceId>> findApprovedResourceIds(TenantId tenantId, ServiceIdentityId identityId, ApplicationId targetApplicationId) {
                return Mono.just(Set.of());
            }

            @Override
            public Mono<Set<String>> findApprovedTechnicalRoleCodes(TenantId tenantId, ServiceIdentityId identityId, ApplicationId targetApplicationId) {
                return Mono.just(Set.of());
            }

            @Override
            public Mono<co.edu.uco.seguridad.pdp.authorization.domain.serviceidentity.ServiceTechnicalRoleDefinition> saveTechnicalRole(co.edu.uco.seguridad.pdp.authorization.domain.serviceidentity.ServiceTechnicalRoleDefinition role) {
                return Mono.just(role);
            }

            @Override
            public Mono<co.edu.uco.seguridad.pdp.authorization.domain.serviceidentity.ServiceTechnicalRoleDefinition> findTechnicalRole(TenantId tenantId, UUID id) {
                return Mono.empty();
            }

            @Override
            public Flux<co.edu.uco.seguridad.pdp.authorization.domain.serviceidentity.ServiceTechnicalRoleDefinition> findTechnicalRoles(TenantId tenantId, ApplicationId applicationId) {
                return Flux.empty();
            }

            @Override
            public Mono<Boolean> hasPendingOrApprovedTechnicalRoleGrant(TenantId tenantId, ServiceIdentityId identityId, UUID roleId) {
                return Mono.just(false);
            }

            @Override
            public Mono<Void> replaceTechnicalRoleResources(TenantId tenantId, UUID roleId, Set<ResourceId> resourceIds) {
                return Mono.empty();
            }

            @Override
            public Mono<Set<ResourceId>> findTechnicalRoleResourceIds(TenantId tenantId, UUID roleId) {
                return Mono.just(Set.of());
            }

            @Override
            public Mono<co.edu.uco.seguridad.pdp.commons.model.ResultPage<co.edu.uco.seguridad.pdp.authorization.domain.serviceidentity.ServiceTechnicalRoleDefinition>> findTechnicalRolesPage(TenantId tenantId, ApplicationId applicationId, co.edu.uco.seguridad.pdp.commons.model.PageWindow window) {
                return Mono.just(co.edu.uco.seguridad.pdp.commons.model.ResultPage.of(java.util.List.of(), 0, window));
            }

            @Override
            public Mono<co.edu.uco.seguridad.pdp.commons.model.ResultPage<ResourceId>> findTechnicalRoleResourceIdsPage(TenantId tenantId, UUID roleId, co.edu.uco.seguridad.pdp.commons.model.PageWindow window) {
                return Mono.just(co.edu.uco.seguridad.pdp.commons.model.ResultPage.of(java.util.List.of(), 0, window));
            }

            @Override
            public Mono<Long> countTechnicalRoleResources(TenantId tenantId, UUID roleId) {
                return Mono.just(0L);
            }

            @Override
            public Mono<Void> addTechnicalRoleResource(TenantId tenantId, UUID roleId, ResourceId resourceId) {
                return Mono.empty();
            }

            @Override
            public Mono<Void> removeTechnicalRoleResource(TenantId tenantId, UUID roleId, ResourceId resourceId) {
                return Mono.empty();
            }

            @Override
            public Mono<Boolean> hasTechnicalRoleResource(TenantId tenantId, UUID roleId, ResourceId resourceId) {
                return Mono.just(false);
            }
        };
    }
}
