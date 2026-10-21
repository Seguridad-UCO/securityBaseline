package co.edu.uco.seguridad.pdp.authorization.application.usecase.impl;

import co.edu.uco.seguridad.pdp.applications.application.rule.validator.ApplicationDetailsLookupValidator;
import co.edu.uco.seguridad.pdp.applications.domain.exception.ApplicationNotFoundException;
import co.edu.uco.seguridad.pdp.assignments.application.primaryport.request.ResolveUserApplicationsRequest;
import co.edu.uco.seguridad.pdp.assignments.application.primaryport.response.UserApplicationAssignmentsResponse;
import co.edu.uco.seguridad.pdp.assignments.application.usecase.ResolveUserApplicationsUseCase;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.ReadCurrentUserApplicationsRequest;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.response.CurrentUserApplicationsResponse;
import co.edu.uco.seguridad.pdp.authorization.application.usecase.ReadCurrentUserApplicationsUseCase;
import co.edu.uco.seguridad.pdp.authorization.domain.exception.CurrentUserApplicationsAccessDeniedException;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.ProfileId;
import co.edu.uco.seguridad.pdp.commons.model.RoleId;
import co.edu.uco.seguridad.pdp.profiles.application.secondaryport.repository.ProfileRepository;
import co.edu.uco.seguridad.pdp.profiles.domain.Profile;
import co.edu.uco.seguridad.pdp.roles.application.secondaryport.repository.RoleRepository;
import co.edu.uco.seguridad.pdp.roles.domain.Role;
import co.edu.uco.seguridad.pdp.roles.domain.model.RoleScopeLevel;
import co.edu.uco.seguridad.pdp.tenants.application.usecase.FindTenantNameByCodeUseCase;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class ReadCurrentUserApplicationsUseCaseImpl implements ReadCurrentUserApplicationsUseCase {
    private final ResolveUserApplicationsUseCase assignments;
    private final FindTenantNameByCodeUseCase tenants;
    private final ApplicationDetailsLookupValidator applications;
    private final RoleRepository roles;
    private final ProfileRepository profiles;

    public ReadCurrentUserApplicationsUseCaseImpl(ResolveUserApplicationsUseCase assignments, FindTenantNameByCodeUseCase tenants,
            ApplicationDetailsLookupValidator applications, RoleRepository roles, ProfileRepository profiles) {
        this.assignments = Objects.requireNonNull(assignments); this.tenants = Objects.requireNonNull(tenants);
        this.applications = Objects.requireNonNull(applications); this.roles = Objects.requireNonNull(roles);
        this.profiles = Objects.requireNonNull(profiles);
    }

    @Override public Mono<CurrentUserApplicationsResponse> execute(ReadCurrentUserApplicationsRequest input) {
        return tenants.execute(input.tenantId()).switchIfEmpty(denied())
                .flatMap(tenant -> assignments.execute(new ResolveUserApplicationsRequest(input.userId(), input.tenantId()))
                        .flatMapMany(all -> Flux.fromIterable(all.assignmentsByApplication().entrySet()))
                        .flatMap(entry -> application(entry.getKey(), entry.getValue(), input))
                        .sort(Comparator.comparing(CurrentUserApplicationsResponse.Application::name))
                        .collectList()
                        .map(apps -> new CurrentUserApplicationsResponse(tenant.name(), input.userName(), input.email(), apps)));
    }

    private Mono<CurrentUserApplicationsResponse.Application> application(ApplicationId applicationId,
            UserApplicationAssignmentsResponse.ApplicationAssignments assigned, ReadCurrentUserApplicationsRequest input) {
        return applications.execute(applicationId).onErrorResume(ApplicationNotFoundException.class, ignored -> denied())
                .filter(application -> application.tenantId().equals(input.tenantId()))
                .switchIfEmpty(denied()).flatMap(application -> Mono.zip(directRoles(assigned.directRoleIds(), applicationId, input),
                        profiles(assigned.profileIds(), applicationId, input)).map(parts -> new CurrentUserApplicationsResponse.Application(
                                application.name().value(), parts.getT1().tenantRoles(), parts.getT1().applicationRoles(), parts.getT2())));
    }

    private Mono<DirectRoles> directRoles(Set<RoleId> roleIds, ApplicationId applicationId, ReadCurrentUserApplicationsRequest input) {
        return visibleRoles(roleIds, applicationId, input).collectList().map(found -> new DirectRoles(
                found.stream().filter(role -> role.scope().level() == RoleScopeLevel.TENANT).map(this::namedRole).toList(),
                found.stream().filter(role -> role.scope().level() == RoleScopeLevel.APPLICATION).map(this::namedRole).toList()));
    }

    private Mono<List<CurrentUserApplicationsResponse.Profile>> profiles(Set<ProfileId> profileIds, ApplicationId applicationId,
            ReadCurrentUserApplicationsRequest input) {
        return Flux.fromIterable(profileIds).flatMap(profileId -> profiles.findByIdForTenant(profileId, input.tenantId())
                        .filter(profile -> belongsToApplication(profile, applicationId)).switchIfEmpty(denied())
                        .flatMap(profile -> visibleRoles(profile.roles(), applicationId, input).map(this::profileRole).collectList()
                                .map(roleViews -> new CurrentUserApplicationsResponse.Profile(profile.name().value(), roleViews))))
                .sort(Comparator.comparing(CurrentUserApplicationsResponse.Profile::name)).collectList();
    }

    private Flux<Role> visibleRoles(Set<RoleId> roleIds, ApplicationId applicationId, ReadCurrentUserApplicationsRequest input) {
        return Flux.fromIterable(roleIds).flatMap(roleId -> roles.findById(roleId).switchIfEmpty(denied()).flatMap(role ->
                role.scope().isGlobal() ? Mono.empty() : validForAssignment(role, applicationId, input) ? Mono.just(role) : denied()))
                .sort(Comparator.comparing(role -> role.name().value()));
    }

    private boolean validForAssignment(Role role, ApplicationId applicationId, ReadCurrentUserApplicationsRequest input) {
        return role.scope().tenantId().filter(input.tenantId()::equals).isPresent() && (role.scope().level() == RoleScopeLevel.TENANT
                || role.scope().applicationId().filter(applicationId::equals).isPresent());
    }

    private boolean belongsToApplication(Profile profile, ApplicationId applicationId) {
        return profile.scope().level() == RoleScopeLevel.TENANT || profile.scope().applicationId().filter(applicationId::equals).isPresent();
    }

    private CurrentUserApplicationsResponse.NamedRole namedRole(Role role) {
        return new CurrentUserApplicationsResponse.NamedRole(role.name().value());
    }

    private CurrentUserApplicationsResponse.ProfileRole profileRole(Role role) {
        return new CurrentUserApplicationsResponse.ProfileRole(role.name().value(), role.scope().level().name());
    }

    private static <T> Mono<T> denied() { return Mono.error(new CurrentUserApplicationsAccessDeniedException()); }
    private record DirectRoles(List<CurrentUserApplicationsResponse.NamedRole> tenantRoles, List<CurrentUserApplicationsResponse.NamedRole> applicationRoles) { }
}
