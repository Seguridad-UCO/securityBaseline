package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.controller;

import co.edu.uco.seguridad.pdp.applications.application.primaryport.request.UpdateApplicationRequest;
import co.edu.uco.seguridad.pdp.applications.application.usecase.UpdateApplicationUseCase;
import co.edu.uco.seguridad.pdp.assignments.application.rule.validator.ProfileDeletionDependencyValidator;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AdministrationRequest;
import co.edu.uco.seguridad.pdp.authorization.application.rule.validator.PrincipalMustBeApplicationAdministratorValidator;
import co.edu.uco.seguridad.pdp.commons.model.*;
import co.edu.uco.seguridad.pdp.identity.application.rule.validator.SubjectUserIdLookupValidator;
import co.edu.uco.seguridad.pdp.profiles.application.primaryport.request.UpdateProfileRequest;
import co.edu.uco.seguridad.pdp.profiles.application.rule.validator.ProfileApplicationLookupValidator;
import co.edu.uco.seguridad.pdp.profiles.application.usecase.RemoveProfileUseCase;
import co.edu.uco.seguridad.pdp.profiles.application.usecase.UpdateProfileUseCase;
import co.edu.uco.seguridad.pdp.resources.application.primaryport.request.UpdateProtectedResourceRequest;
import co.edu.uco.seguridad.pdp.resources.application.usecase.RemoveProtectedResourceUseCase;
import co.edu.uco.seguridad.pdp.resources.application.usecase.UpdateProtectedResourceUseCase;
import co.edu.uco.seguridad.pdp.roles.application.primaryport.request.UpdateRoleRequest;
import co.edu.uco.seguridad.pdp.roles.application.rule.validator.ResourceDeletionDependencyValidator;
import co.edu.uco.seguridad.pdp.roles.application.rule.validator.RoleApplicationLookupValidator;
import co.edu.uco.seguridad.pdp.roles.application.usecase.RemoveRoleUseCase;
import co.edu.uco.seguridad.pdp.roles.application.usecase.UpdateRoleUseCase;
import co.edu.uco.seguridad.shared.security.PdpPrincipal;
import co.edu.uco.seguridad.shared.security.SecurityContext;
import co.edu.uco.seguridad.shared.web.ApiResponse;
import co.edu.uco.seguridad.shared.web.CorrelationWebFilter;
import co.edu.uco.seguridad.shared.web.RequestContext;
import co.edu.uco.seguridad.shared.web.RequestFieldParser;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Optional;
import java.util.Set;

/**
 * PATCH/DELETE administrativos. Las asociaciones tienen sus propios endpoints especializados.
 */
@RestController
final class CatalogMaintenanceController {
    private final PrincipalMustBeApplicationAdministratorValidator administrator;
    private final SubjectUserIdLookupValidator users;
    private final UpdateApplicationUseCase applications;
    private final UpdateProtectedResourceUseCase resources;
    private final RemoveProtectedResourceUseCase removeResources;
    private final UpdateRoleUseCase roles;
    private final RemoveRoleUseCase removeRoles;
    private final UpdateProfileUseCase profiles;
    private final RemoveProfileUseCase removeProfiles;
    private final RoleApplicationLookupValidator roleApplication;
    private final ProfileApplicationLookupValidator profileApplication;
    private final ResourceDeletionDependencyValidator resourceDependencies;
    private final co.edu.uco.seguridad.pdp.profiles.application.rule.validator.RoleDeletionDependencyValidator profileRoleDependencies;
    private final co.edu.uco.seguridad.pdp.assignments.application.rule.validator.RoleDeletionDependencyValidator assignmentRoleDependencies;
    private final ProfileDeletionDependencyValidator profileDependencies;

    CatalogMaintenanceController(PrincipalMustBeApplicationAdministratorValidator administrator, SubjectUserIdLookupValidator users,
                                 UpdateApplicationUseCase applications, UpdateProtectedResourceUseCase resources, RemoveProtectedResourceUseCase removeResources,
                                 UpdateRoleUseCase roles, RemoveRoleUseCase removeRoles, UpdateProfileUseCase profiles, RemoveProfileUseCase removeProfiles,
                                 RoleApplicationLookupValidator roleApplication, ProfileApplicationLookupValidator profileApplication,
                                 ResourceDeletionDependencyValidator resourceDependencies,
                                 co.edu.uco.seguridad.pdp.profiles.application.rule.validator.RoleDeletionDependencyValidator profileRoleDependencies,
                                 co.edu.uco.seguridad.pdp.assignments.application.rule.validator.RoleDeletionDependencyValidator assignmentRoleDependencies,
                                 ProfileDeletionDependencyValidator profileDependencies) {
        this.administrator = administrator;
        this.users = users;
        this.applications = applications;
        this.resources = resources;
        this.removeResources = removeResources;
        this.roles = roles;
        this.removeRoles = removeRoles;
        this.profiles = profiles;
        this.removeProfiles = removeProfiles;
        this.roleApplication = roleApplication;
        this.profileApplication = profileApplication;
        this.resourceDependencies = resourceDependencies;
        this.profileRoleDependencies = profileRoleDependencies;
        this.assignmentRoleDependencies = assignmentRoleDependencies;
        this.profileDependencies = profileDependencies;
    }

    @PatchMapping("/api/v1/applications/{applicationId}")
    Mono<ResponseEntity<ApiResponse<Object>>> updateApplication(@PathVariable String applicationId, @RequestBody ApplicationPatch body, ServerWebExchange exchange) {
        ApplicationId id = id(applicationId);
        return principal().flatMap(p -> gate(p, id).then(applications.execute(new UpdateApplicationRequest(p.tenantId(), id,
                RequestFieldParser.parse("name", body.name(), co.edu.uco.seguridad.pdp.commons.model.ApplicationName::new), body.description(),
                RequestFieldParser.parse("baseUrl", body.baseUrl(), co.edu.uco.seguridad.pdp.applications.domain.model.ApplicationBaseUrl::new))))).map(r -> ok("APPLICATION_UPDATED", r, exchange));
    }

    @PatchMapping("/api/v1/applications/{applicationId}/resources/{resourceId}")
    Mono<ResponseEntity<ApiResponse<Object>>> updateResource(@PathVariable String applicationId, @PathVariable String resourceId, @RequestBody ResourcePatch body, ServerWebExchange exchange) {
        ApplicationId app = id(applicationId);
        return principal().flatMap(p -> gate(p, app).then(resources.execute(new UpdateProtectedResourceRequest(p.tenantId(), rid(resourceId),
                RequestFieldParser.parse("path", body.path(), co.edu.uco.seguridad.pdp.resources.domain.model.ResourcePath::new), RequestFieldParser.parse("method", body.method(), co.edu.uco.seguridad.pdp.resources.domain.model.HttpVerb::parse))))).map(r -> ok("RESOURCE_UPDATED", r, exchange));
    }

    @DeleteMapping("/api/v1/applications/{applicationId}/resources/{resourceId}")
    Mono<ResponseEntity<ApiResponse<Object>>> deleteResource(@PathVariable String applicationId, @PathVariable String resourceId, ServerWebExchange exchange) {
        ApplicationId app = id(applicationId);
        ResourceId resource = rid(resourceId);
        return principal().flatMap(p -> gate(p, app).then(resourceDependencies.execute(resource)).then(removeResources.execute(new RemoveProtectedResourceUseCase.Request(p.tenantId(), resource)))).thenReturn(ok("RESOURCE_REMOVED", null, exchange));
    }

    @PatchMapping("/api/v1/roles/{roleId}")
    Mono<ResponseEntity<ApiResponse<Object>>> updateRole(@PathVariable String roleId, @RequestBody NamePatch body, ServerWebExchange exchange) {
        RoleId role = role(roleId);
        return principal().flatMap(p -> roleApplication.execute(new co.edu.uco.seguridad.pdp.roles.application.primaryport.request.RoleOwnershipQuery(role, p.tenantId())).flatMap(app -> gateOptional(p, app).then(roles.execute(new UpdateRoleRequest(p.tenantId(), role, RequestFieldParser.parse("name", body.name(), co.edu.uco.seguridad.pdp.roles.domain.model.RoleName::new)))))).map(r -> ok("ROLE_UPDATED", r, exchange));
    }

    @DeleteMapping("/api/v1/roles/{roleId}")
    Mono<ResponseEntity<ApiResponse<Object>>> deleteRole(@PathVariable String roleId, ServerWebExchange exchange) {
        RoleId role = role(roleId);
        return principal().flatMap(p -> roleApplication.execute(new co.edu.uco.seguridad.pdp.roles.application.primaryport.request.RoleOwnershipQuery(role, p.tenantId())).flatMap(app -> gateOptional(p, app).then(profileRoleDependencies.execute(role)).then(assignmentRoleDependencies.execute(role)).then(removeRoles.execute(new RemoveRoleUseCase.Request(p.tenantId(), role))))).thenReturn(ok("ROLE_REMOVED", null, exchange));
    }

    @PatchMapping("/api/v1/profiles/{profileId}")
    Mono<ResponseEntity<ApiResponse<Object>>> updateProfile(@PathVariable String profileId, @RequestBody NamePatch body, ServerWebExchange exchange) {
        ProfileId profile = profile(profileId);
        return principal().flatMap(p -> profileApplication.execute(new co.edu.uco.seguridad.pdp.profiles.application.primaryport.request.ProfileOwnershipQuery(p.tenantId(), profile)).flatMap(app -> gateOptional(p, app).then(profiles.execute(new UpdateProfileRequest(p.tenantId(), profile, RequestFieldParser.parse("name", body.name(), co.edu.uco.seguridad.pdp.profiles.domain.model.ProfileName::new)))))).map(r -> ok("PROFILE_UPDATED", r, exchange));
    }

    @DeleteMapping("/api/v1/profiles/{profileId}")
    Mono<ResponseEntity<ApiResponse<Object>>> deleteProfile(@PathVariable String profileId, ServerWebExchange exchange) {
        ProfileId profile = profile(profileId);
        return principal().flatMap(p -> profileApplication.execute(new co.edu.uco.seguridad.pdp.profiles.application.primaryport.request.ProfileOwnershipQuery(p.tenantId(), profile)).flatMap(app -> gateOptional(p, app).then(profileDependencies.execute(profile)).then(removeProfiles.execute(new RemoveProfileUseCase.Request(p.tenantId(), profile))))).thenReturn(ok("PROFILE_REMOVED", null, exchange));
    }

    private Mono<PdpPrincipal> principal() {
        return SecurityContext.currentPrincipal();
    }

    private Mono<Void> gate(PdpPrincipal p, ApplicationId app) {
        return user(p).flatMap(u -> administrator.execute(new AdministrationRequest(p.tenantId(), app, u, p.subject(), Set.of(), p.authenticationContext())));
    }

    private Mono<Void> gateOptional(PdpPrincipal p, Optional<ApplicationId> app) {
        return app.map(a -> gate(p, a)).orElseGet(Mono::empty);
    }

    private Mono<UserId> user(PdpPrincipal p) {
        return p.userId().map(Mono::just).orElseGet(() -> users.execute(p.subject()));
    }

    private static ApplicationId id(String value) {
        return RequestFieldParser.parse("applicationId", value, ApplicationId::of);
    }

    private static ResourceId rid(String value) {
        return RequestFieldParser.parse("resourceId", value, ResourceId::of);
    }

    private static RoleId role(String value) {
        return RequestFieldParser.parse("roleId", value, RoleId::of);
    }

    private static ProfileId profile(String value) {
        return RequestFieldParser.parse("profileId", value, ProfileId::of);
    }

    private static ResponseEntity<ApiResponse<Object>> ok(String code, Object body, ServerWebExchange exchange) {
        RequestContext context = CorrelationWebFilter.context(exchange);
        return ResponseEntity.ok(ApiResponse.success(code, code, body, context));
    }

    record ApplicationPatch(String name, String description, String baseUrl) {
    }

    record ResourcePatch(String path, String method) {
    }

    record NamePatch(String name) {
    }
}
