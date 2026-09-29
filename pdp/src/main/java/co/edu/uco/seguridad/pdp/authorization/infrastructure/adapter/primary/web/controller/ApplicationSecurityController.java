package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.controller;

import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.request.raw.ListApplicationSecurityRawRequest;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.request.raw.ListApplicationSecurityRelationRawRequest;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.request.raw.SearchApplicationSecurityUsersRawRequest;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.response.ApplicationAdministratorWebResponse;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.response.ApplicationSecurityProfileAssignmentWebResponse;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.response.ApplicationSecurityProfileWebResponse;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.response.ApplicationSecurityResourceWebResponse;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.response.ApplicationSecurityRoleAssignmentWebResponse;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.response.ApplicationSecurityRoleWebResponse;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.response.ApplicationSecuritySummaryWebResponse;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.response.ApplicationSecurityUserWebResponse;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor.ListApplicationProfileAssignmentsInteractor;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor.ListApplicationRoleAssignmentsInteractor;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor.ListApplicationSecurityAdministratorsInteractor;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor.ListApplicationSecurityProfilesInteractor;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor.ListApplicationSecurityResourcesInteractor;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor.ListApplicationSecurityRolesInteractor;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor.ListProfileRolesInteractor;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor.ListRoleResourcesInteractor;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor.ReadApplicationSecuritySummaryInteractor;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor.SearchApplicationSecurityUsersInteractor;
import co.edu.uco.seguridad.shared.web.ApiResponse;
import co.edu.uco.seguridad.shared.web.CorrelationWebFilter;
import co.edu.uco.seguridad.shared.web.PageResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Primary adapter for the application-scoped security administration read model.
 * Authorization, tenant resolution and pagination mapping belong to its interactors.
 */
@RestController
@RequestMapping("/api/v1/applications/{applicationId}/security")
final class ApplicationSecurityController {
    private final ReadApplicationSecuritySummaryInteractor summary;
    private final ListApplicationSecurityResourcesInteractor resources;
    private final ListApplicationSecurityRolesInteractor roles;
    private final ListApplicationSecurityProfilesInteractor profiles;
    private final ListApplicationSecurityAdministratorsInteractor administrators;
    private final ListApplicationRoleAssignmentsInteractor roleAssignments;
    private final ListApplicationProfileAssignmentsInteractor profileAssignments;
    private final SearchApplicationSecurityUsersInteractor users;
    private final ListRoleResourcesInteractor roleResources;
    private final ListProfileRolesInteractor profileRoles;

    ApplicationSecurityController(
            ReadApplicationSecuritySummaryInteractor summary,
            ListApplicationSecurityResourcesInteractor resources,
            ListApplicationSecurityRolesInteractor roles,
            ListApplicationSecurityProfilesInteractor profiles,
            ListApplicationSecurityAdministratorsInteractor administrators,
            ListApplicationRoleAssignmentsInteractor roleAssignments,
            ListApplicationProfileAssignmentsInteractor profileAssignments,
            SearchApplicationSecurityUsersInteractor users, ListRoleResourcesInteractor roleResources,
            ListProfileRolesInteractor profileRoles) {
        this.summary = summary;
        this.resources = resources;
        this.roles = roles;
        this.profiles = profiles;
        this.administrators = administrators;
        this.roleAssignments = roleAssignments;
        this.profileAssignments = profileAssignments;
        this.users = users;
        this.roleResources = roleResources;
        this.profileRoles = profileRoles;
    }

    @GetMapping("/summary")
    Mono<ResponseEntity<ApiResponse<ApplicationSecuritySummaryWebResponse>>> summary(
            @PathVariable String applicationId, ServerWebExchange exchange) {
        return summary.execute(applicationId)
                .map(data -> ok("APPLICATION_SECURITY_SUMMARY_READ", data, exchange));
    }

    @GetMapping("/resources")
    Mono<ResponseEntity<ApiResponse<PageResponse<ApplicationSecurityResourceWebResponse>>>> resources(
            @PathVariable String applicationId, @RequestParam(required = false) String page,
            @RequestParam(required = false) String size, @RequestParam(required = false) String offset,
            @RequestParam(required = false) String limit, ServerWebExchange exchange) {
        return resources.execute(pageRequest(applicationId, page, size, offset, limit))
                .map(data -> ok("APPLICATION_SECURITY_RESOURCES_LISTED", data, exchange));
    }

    @GetMapping("/roles")
    Mono<ResponseEntity<ApiResponse<PageResponse<ApplicationSecurityRoleWebResponse>>>> roles(
            @PathVariable String applicationId, @RequestParam(required = false) String page,
            @RequestParam(required = false) String size, @RequestParam(required = false) String offset,
            @RequestParam(required = false) String limit, ServerWebExchange exchange) {
        return roles.execute(pageRequest(applicationId, page, size, offset, limit))
                .map(data -> ok("APPLICATION_SECURITY_ROLES_LISTED", data, exchange));
    }

    @GetMapping("/profiles")
    Mono<ResponseEntity<ApiResponse<PageResponse<ApplicationSecurityProfileWebResponse>>>> profiles(
            @PathVariable String applicationId, @RequestParam(required = false) String page,
            @RequestParam(required = false) String size, @RequestParam(required = false) String offset,
            @RequestParam(required = false) String limit, ServerWebExchange exchange) {
        return profiles.execute(pageRequest(applicationId, page, size, offset, limit))
                .map(data -> ok("APPLICATION_SECURITY_PROFILES_LISTED", data, exchange));
    }

    @GetMapping("/administrators")
    Mono<ResponseEntity<ApiResponse<PageResponse<ApplicationAdministratorWebResponse>>>> administrators(
            @PathVariable String applicationId, @RequestParam(required = false) String page,
            @RequestParam(required = false) String size, @RequestParam(required = false) String offset,
            @RequestParam(required = false) String limit, ServerWebExchange exchange) {
        return administrators.execute(pageRequest(applicationId, page, size, offset, limit))
                .map(data -> ok("APPLICATION_SECURITY_ADMINISTRATORS_LISTED", data, exchange));
    }

    @GetMapping("/role-assignments")
    Mono<ResponseEntity<ApiResponse<PageResponse<ApplicationSecurityRoleAssignmentWebResponse>>>> roleAssignments(
            @PathVariable String applicationId, @RequestParam(required = false) String page,
            @RequestParam(required = false) String size, @RequestParam(required = false) String offset,
            @RequestParam(required = false) String limit, ServerWebExchange exchange) {
        return roleAssignments.execute(pageRequest(applicationId, page, size, offset, limit))
                .map(data -> ok("APPLICATION_SECURITY_ROLE_ASSIGNMENTS_LISTED", data, exchange));
    }

    @GetMapping("/profile-assignments")
    Mono<ResponseEntity<ApiResponse<PageResponse<ApplicationSecurityProfileAssignmentWebResponse>>>> profileAssignments(
            @PathVariable String applicationId, @RequestParam(required = false) String page,
            @RequestParam(required = false) String size, @RequestParam(required = false) String offset,
            @RequestParam(required = false) String limit, ServerWebExchange exchange) {
        return profileAssignments.execute(pageRequest(applicationId, page, size, offset, limit))
                .map(data -> ok("APPLICATION_SECURITY_PROFILE_ASSIGNMENTS_LISTED", data, exchange));
    }

    @GetMapping("/roles/{roleId}/resources")
    Mono<ResponseEntity<ApiResponse<PageResponse<ApplicationSecurityResourceWebResponse>>>> roleResources(
            @PathVariable String applicationId, @PathVariable String roleId, @RequestParam(required = false) String page,
            @RequestParam(required = false) String size, @RequestParam(required = false) String offset,
            @RequestParam(required = false) String limit, ServerWebExchange exchange) {
        return roleResources.execute(new ListApplicationSecurityRelationRawRequest(applicationId, roleId, page, size, offset, limit))
                .map(data -> ok("APPLICATION_SECURITY_ROLE_RESOURCES_LISTED", data, exchange));
    }

    @GetMapping("/profiles/{profileId}/roles")
    Mono<ResponseEntity<ApiResponse<PageResponse<ApplicationSecurityRoleWebResponse>>>> profileRoles(
            @PathVariable String applicationId, @PathVariable String profileId, @RequestParam(required = false) String page,
            @RequestParam(required = false) String size, @RequestParam(required = false) String offset,
            @RequestParam(required = false) String limit, ServerWebExchange exchange) {
        return profileRoles.execute(new ListApplicationSecurityRelationRawRequest(applicationId, profileId, page, size, offset, limit))
                .map(data -> ok("APPLICATION_SECURITY_PROFILE_ROLES_LISTED", data, exchange));
    }

    @GetMapping("/users")
    Mono<ResponseEntity<ApiResponse<PageResponse<ApplicationSecurityUserWebResponse>>>> users(
            @PathVariable String applicationId, @RequestParam(defaultValue = "") String query,
            @RequestParam(required = false) String page, @RequestParam(required = false) String size,
            @RequestParam(required = false) String offset, @RequestParam(required = false) String limit,
            ServerWebExchange exchange) {
        return users.execute(new SearchApplicationSecurityUsersRawRequest(applicationId, query, page, size, offset, limit))
                .map(data -> ok("APPLICATION_SECURITY_USERS_LISTED", data, exchange));
    }

    private static ListApplicationSecurityRawRequest pageRequest(String applicationId, String page, String size,
                                                                   String offset, String limit) {
        return new ListApplicationSecurityRawRequest(applicationId, page, size, offset, limit);
    }

    private static <T> ResponseEntity<ApiResponse<T>> ok(String code, T data, ServerWebExchange exchange) {
        return ResponseEntity.ok(ApiResponse.success(code, code, data, CorrelationWebFilter.context(exchange)));
    }
}
