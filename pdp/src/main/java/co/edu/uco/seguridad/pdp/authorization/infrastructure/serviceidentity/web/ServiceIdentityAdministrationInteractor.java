package co.edu.uco.seguridad.pdp.authorization.infrastructure.serviceidentity.web;

import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AdministrationRequest;
import co.edu.uco.seguridad.pdp.authorization.application.rule.validator.PrincipalMustBeApplicationAdministratorValidator;
import co.edu.uco.seguridad.pdp.authorization.application.serviceidentity.port.ServiceIdentityRepository;
import co.edu.uco.seguridad.pdp.authorization.application.serviceidentity.request.*;
import co.edu.uco.seguridad.pdp.authorization.application.serviceidentity.response.ServiceIdentityResponse;
import co.edu.uco.seguridad.pdp.authorization.application.serviceidentity.usecase.*;
import co.edu.uco.seguridad.pdp.authorization.domain.serviceidentity.ServiceAccessGrant;
import co.edu.uco.seguridad.pdp.authorization.domain.serviceidentity.ServiceIdentityId;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.request.raw.ApplicationAdministrationRawRequest;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.request.raw.ApplicationSecurityPageRawRequest;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.mapper.ApplicationAdministrationRequestMapper;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.mapper.ApplicationSecurityPageRequestMapper;
import co.edu.uco.seguridad.pdp.commons.model.PageWindow;
import co.edu.uco.seguridad.pdp.commons.model.ResourceId;
import co.edu.uco.seguridad.pdp.commons.model.UserId;
import co.edu.uco.seguridad.pdp.identity.application.rule.validator.SubjectUserIdLookupValidator;
import co.edu.uco.seguridad.pdp.resources.application.secondaryport.repository.ProtectedResourceRepository;
import co.edu.uco.seguridad.shared.security.PdpPrincipal;
import co.edu.uco.seguridad.shared.security.SecurityContext;
import co.edu.uco.seguridad.shared.web.PageResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Adaptador de transporte: la identidad humana se deriva del contexto, jamás del cuerpo.
 */
public final class ServiceIdentityAdministrationInteractor {
    private final CreateServiceIdentityUseCase create;
    private final RequestServiceAccessUseCase request;
    private final RequestServiceTechnicalRoleAccessUseCase requestRole;
    private final CreateServiceTechnicalRoleUseCase createRole;
    private final AddServiceTechnicalRoleResourceUseCase addRoleResource;
    private final RemoveServiceTechnicalRoleResourceUseCase removeRoleResource;
    private final DecideServiceAccessUseCase decide;
    private final RevokeServiceAccessUseCase revoke;
    private final SubjectUserIdLookupValidator users;
    private final PrincipalMustBeApplicationAdministratorValidator administrators;
    private final ServiceIdentityRepository identities;
    private final ProtectedResourceRepository resources;

    public ServiceIdentityAdministrationInteractor(CreateServiceIdentityUseCase create, RequestServiceAccessUseCase request, RequestServiceTechnicalRoleAccessUseCase requestRole, CreateServiceTechnicalRoleUseCase createRole, AddServiceTechnicalRoleResourceUseCase addRoleResource, RemoveServiceTechnicalRoleResourceUseCase removeRoleResource, DecideServiceAccessUseCase decide, RevokeServiceAccessUseCase revoke, SubjectUserIdLookupValidator users, PrincipalMustBeApplicationAdministratorValidator administrators, ServiceIdentityRepository identities, ProtectedResourceRepository resources) {
        this.create = create;
        this.request = request;
        this.requestRole = requestRole;
        this.createRole = createRole;
        this.addRoleResource = addRoleResource;
        this.removeRoleResource = removeRoleResource;
        this.decide = decide;
        this.revoke = revoke;
        this.users = users;
        this.administrators = administrators;
        this.identities = identities;
        this.resources = resources;
    }

    public Mono<ServiceIdentityResponse> create(String applicationId, CreateServiceIdentityRawRequest raw) {
        return administration(applicationId).flatMap(administration -> create.execute(new CreateServiceIdentityRequest(administration, raw.name())));
    }

    public Mono<ServiceAccessGrant> request(String applicationId, String identityId, RequestServiceAccessRawRequest raw) {
        return administration(applicationId).flatMap(administration -> raw.technicalRoleId() != null && !raw.technicalRoleId().isBlank() ? requestRole.execute(new RequestServiceTechnicalRoleAccessRequest(administration, ServiceIdentityId.of(identityId), UUID.fromString(raw.technicalRoleId()))) : request.execute(new RequestServiceAccessRequest(administration, ServiceIdentityId.of(identityId), ResourceId.of(raw.resourceId()), "SERVICE_CONSUMER")));
    }

    public Mono<ServiceTechnicalRoleWebResponse> createTechnicalRole(String applicationId, CreateServiceTechnicalRoleRawRequest raw) {
        return administration(applicationId).flatMap(administration -> createRole.execute(new CreateServiceTechnicalRoleRequest(administration, raw.code(), raw.name(), raw.resourceIds() == null ? Set.of() : raw.resourceIds().stream().map(ResourceId::of).collect(Collectors.toSet())))).flatMap(role -> identities.findTechnicalRoleResourceIds(role.tenantId(), role.id()).map(resources -> ServiceTechnicalRoleWebResponse.from(role, resources.stream().map(resource -> resource.value().toString()).collect(Collectors.toSet()))));
    }

    public Mono<PageResponse<ServiceTechnicalRoleSummaryWebResponse>> technicalRoles(String applicationId, String page, String size, String offset, String limit) {
        PageWindow window = window(page, size, offset, limit);
        return administration(applicationId).flatMap(administration -> administrators.execute(administration).then(identities.findTechnicalRolesPage(administration.tenantId(), administration.applicationId(), window)).flatMap(result -> Flux.fromIterable(result.content()).concatMap(role -> identities.countTechnicalRoleResources(administration.tenantId(), role.id()).map(count -> ServiceTechnicalRoleSummaryWebResponse.from(role, count))).collectList().map(content -> new PageResponse<>(content, result.total(), result.window().page(), result.window().offset(), result.window().limit()))));
    }

    public Mono<PageResponse<ServiceTechnicalRoleResourceWebResponse>> technicalRoleResources(String applicationId, String roleId, String page, String size, String offset, String limit) {
        PageWindow window = window(page, size, offset, limit);
        return administration(applicationId).flatMap(administration -> administrators.execute(administration).then(identities.findTechnicalRole(administration.tenantId(), UUID.fromString(roleId)).filter(role -> role.applicationId().equals(administration.applicationId())).switchIfEmpty(Mono.error(new IllegalArgumentException("el rol técnico no pertenece a la aplicación administrada"))).then(identities.findTechnicalRoleResourceIdsPage(administration.tenantId(), UUID.fromString(roleId), window))).flatMap(result -> Flux.fromIterable(result.content()).concatMap(resourceId -> resources.findByIdForTenant(resourceId, administration.tenantId()).map(ServiceTechnicalRoleResourceWebResponse::from)).collectList().map(content -> new PageResponse<>(content, result.total(), result.window().page(), result.window().offset(), result.window().limit()))));
    }

    public Mono<Void> addTechnicalRoleResource(String applicationId, String roleId, ChangeServiceTechnicalRoleResourceRawRequest raw) {
        return administration(applicationId).flatMap(administration -> addRoleResource.execute(new ChangeServiceTechnicalRoleResourceRequest(administration, UUID.fromString(roleId), ResourceId.of(raw.resourceId()))));
    }

    public Mono<Void> removeTechnicalRoleResource(String applicationId, String roleId, String resourceId) {
        return administration(applicationId).flatMap(administration -> removeRoleResource.execute(new ChangeServiceTechnicalRoleResourceRequest(administration, UUID.fromString(roleId), ResourceId.of(resourceId))));
    }

    private static PageWindow window(String page, String size, String offset, String limit) {
        return ApplicationSecurityPageRequestMapper.toWindow(new ApplicationSecurityPageRawRequest(page, size, offset, limit));
    }

    public Mono<ServiceAccessGrant> decide(String targetApplicationId, String grantId, DecideServiceAccessRawRequest raw) {
        return administration(targetApplicationId).flatMap(administration -> decide.execute(new DecideServiceAccessRequest(administration, UUID.fromString(grantId), raw.approve())));
    }

    public Mono<ServiceAccessGrant> revoke(String targetApplicationId, String grantId) {
        return administration(targetApplicationId).flatMap(administration -> revoke.execute(new RevokeServiceAccessRequest(administration, UUID.fromString(grantId))));
    }

    public Flux<ServiceIdentityWebResponse> identities(String applicationId) {
        return administration(applicationId).flatMapMany(administration -> administrators.execute(administration).thenMany(identities.findByConsumer(administration.tenantId(), administration.applicationId()))).map(ServiceIdentityWebResponse::from);
    }

    public Flux<ServiceAccessGrantWebResponse> grants(String applicationId, String identityId) {
        return administration(applicationId).flatMapMany(administration -> administrators.execute(administration).thenMany(identities.findIdentity(administration.tenantId(), ServiceIdentityId.of(identityId)).filter(identity -> identity.consumerApplicationId().equals(administration.applicationId())).flatMapMany(identity -> identities.findGrants(administration.tenantId(), identity.id())))).map(ServiceAccessGrantWebResponse::from);
    }

    public Flux<ServiceAccessGrantWebResponse> targetGrants(String applicationId) {
        return administration(applicationId).flatMapMany(administration -> administrators.execute(administration).thenMany(identities.findGrantsForTarget(administration.tenantId(), administration.applicationId()))).map(ServiceAccessGrantWebResponse::from);
    }

    private Mono<AdministrationRequest> administration(String applicationId) {
        return SecurityContext.currentPrincipal().flatMap(principal -> user(principal).map(user -> ApplicationAdministrationRequestMapper.toAdministrationRequest(new ApplicationAdministrationRawRequest(applicationId), principal, user)));
    }

    private Mono<UserId> user(PdpPrincipal principal) {
        return principal.userId().map(Mono::just).orElseGet(() -> users.execute(principal.subject())).switchIfEmpty(Mono.error(new IllegalStateException("no fue posible resolver el usuario administrador")));
    }
}
