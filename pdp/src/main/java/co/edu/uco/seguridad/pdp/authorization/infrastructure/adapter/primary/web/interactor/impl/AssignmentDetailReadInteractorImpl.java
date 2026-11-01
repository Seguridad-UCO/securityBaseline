package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor.impl;

import co.edu.uco.seguridad.pdp.applications.application.rule.validator.ApplicationOwnerLookupValidator;
import co.edu.uco.seguridad.pdp.assignments.application.primaryport.response.AssignmentResponse;
import co.edu.uco.seguridad.pdp.assignments.application.primaryport.response.ProfileAssignmentResponse;
import co.edu.uco.seguridad.pdp.assignments.application.primaryport.request.ListProfileApplicationAssignmentsPageRequest;
import co.edu.uco.seguridad.pdp.assignments.application.primaryport.request.ListRoleApplicationAssignmentsPageRequest;
import co.edu.uco.seguridad.pdp.assignments.application.primaryport.request.ListUserApplicationAssignmentsPageRequest;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AdministrationRequest;
import co.edu.uco.seguridad.pdp.assignments.application.usecase.ListProfileApplicationAssignmentsPageUseCase;
import co.edu.uco.seguridad.pdp.assignments.application.usecase.ListRoleApplicationAssignmentsPageUseCase;
import co.edu.uco.seguridad.pdp.assignments.application.usecase.ListUserApplicationProfileAssignmentsPageUseCase;
import co.edu.uco.seguridad.pdp.assignments.application.usecase.ListUserApplicationRoleAssignmentsPageUseCase;
import co.edu.uco.seguridad.pdp.authorization.application.rule.validator.PrincipalMustBeApplicationAdministratorValidator;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.request.raw.ApplicationSecurityPageRawRequest;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.request.raw.ListApplicationSecurityRelationRawRequest;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.response.ApplicationSecurityProfileAssignmentWebResponse;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.response.ApplicationSecurityProfileWebResponse;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.response.ApplicationSecurityRoleAssignmentWebResponse;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.response.ApplicationSecurityRoleWebResponse;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.response.ApplicationSecurityUserAssignmentsWebResponse;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.response.ApplicationSecurityUserWebResponse;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor.AssignmentDetailReadInteractor;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.mapper.ApplicationSecurityPageRequestMapper;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.PageWindow;
import co.edu.uco.seguridad.pdp.commons.model.ProfileId;
import co.edu.uco.seguridad.pdp.commons.model.ResultPage;
import co.edu.uco.seguridad.pdp.commons.model.RoleId;
import co.edu.uco.seguridad.pdp.commons.model.UserId;
import co.edu.uco.seguridad.pdp.identity.application.rule.validator.SubjectUserIdLookupValidator;
import co.edu.uco.seguridad.pdp.identity.application.usecase.FindUserByIdUseCase;
import co.edu.uco.seguridad.pdp.profiles.application.primaryport.request.FindProfileForTenantRequest;
import co.edu.uco.seguridad.pdp.profiles.application.usecase.FindProfileForTenantUseCase;
import co.edu.uco.seguridad.pdp.roles.application.primaryport.request.FindRoleForTenantRequest;
import co.edu.uco.seguridad.pdp.roles.application.usecase.FindRoleForTenantUseCase;
import co.edu.uco.seguridad.shared.security.PdpPrincipal;
import co.edu.uco.seguridad.shared.security.SecurityContext;
import co.edu.uco.seguridad.shared.web.PageResponse;
import co.edu.uco.seguridad.shared.web.RequestFieldParser;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Set;

/** Compone vistas de detalle usando únicamente las APIs públicas de los módulos dueños. */
public final class AssignmentDetailReadInteractorImpl implements AssignmentDetailReadInteractor {
    private final ApplicationOwnerLookupValidator owner;
    private final SubjectUserIdLookupValidator subjects;
    private final PrincipalMustBeApplicationAdministratorValidator gate;
    private final ListRoleApplicationAssignmentsPageUseCase roleAssignments;
    private final ListProfileApplicationAssignmentsPageUseCase profileAssignments;
    private final ListUserApplicationRoleAssignmentsPageUseCase userRoleAssignments;
    private final ListUserApplicationProfileAssignmentsPageUseCase userProfileAssignments;
    private final FindUserByIdUseCase users;
    private final FindRoleForTenantUseCase roles;
    private final FindProfileForTenantUseCase profiles;

    public AssignmentDetailReadInteractorImpl(ApplicationOwnerLookupValidator owner,
                                               SubjectUserIdLookupValidator subjects,
                                               PrincipalMustBeApplicationAdministratorValidator gate,
                                               ListRoleApplicationAssignmentsPageUseCase roleAssignments,
                                               ListProfileApplicationAssignmentsPageUseCase profileAssignments,
                                               ListUserApplicationRoleAssignmentsPageUseCase userRoleAssignments,
                                               ListUserApplicationProfileAssignmentsPageUseCase userProfileAssignments,
                                               FindUserByIdUseCase users, FindRoleForTenantUseCase roles,
                                               FindProfileForTenantUseCase profiles) {
        this.owner = owner;
        this.subjects = subjects;
        this.gate = gate;
        this.roleAssignments = roleAssignments;
        this.profileAssignments = profileAssignments;
        this.userRoleAssignments = userRoleAssignments;
        this.userProfileAssignments = userProfileAssignments;
        this.users = users;
        this.roles = roles;
        this.profiles = profiles;
    }

    @Override
    public Mono<PageResponse<ApplicationSecurityRoleAssignmentWebResponse>> roles(ListApplicationSecurityRelationRawRequest raw) {
        RoleId roleId = RequestFieldParser.parse("roleId", raw.entityId(), RoleId::of);
        return administration(raw.applicationId()).flatMap(administration -> gate.execute(administration).then(
                roleAssignments.execute(new ListRoleApplicationAssignmentsPageRequest(administration.tenantId(), administration.applicationId(), roleId, window(raw))))
                .flatMap(page -> rolePage(page, administration)));
    }

    @Override
    public Mono<PageResponse<ApplicationSecurityProfileAssignmentWebResponse>> profiles(ListApplicationSecurityRelationRawRequest raw) {
        ProfileId profileId = RequestFieldParser.parse("profileId", raw.entityId(), ProfileId::of);
        return administration(raw.applicationId()).flatMap(administration -> gate.execute(administration).then(
                profileAssignments.execute(new ListProfileApplicationAssignmentsPageRequest(administration.tenantId(), administration.applicationId(), profileId, window(raw))))
                .flatMap(page -> profilePage(page, administration)));
    }

    @Override
    public Mono<ApplicationSecurityUserAssignmentsWebResponse> user(ListApplicationSecurityRelationRawRequest raw) {
        UserId userId = RequestFieldParser.parse("userId", raw.entityId(), UserId::of);
        return administration(raw.applicationId()).flatMap(administration -> gate.execute(administration).then(users.execute(userId))
                .filter(user -> user.tenantId().equals(administration.tenantId()))
                .switchIfEmpty(Mono.error(new IllegalArgumentException("La persona no pertenece a esta aplicación")))
                .flatMap(user -> Mono.zip(userRoleAssignments.execute(
                                new ListUserApplicationAssignmentsPageRequest(administration.tenantId(), administration.applicationId(), userId, window(raw)))
                                .flatMap(page -> rolePage(page, administration)),
                        userProfileAssignments.execute(new ListUserApplicationAssignmentsPageRequest(administration.tenantId(), administration.applicationId(), userId, window(raw)))
                                .flatMap(page -> profilePage(page, administration)))
                        .map(pages -> new ApplicationSecurityUserAssignmentsWebResponse(user(user), pages.getT1(), pages.getT2()))));
    }

    private Mono<AdministrationRequest> administration(String rawApplicationId) {
        ApplicationId applicationId = RequestFieldParser.parse("applicationId", rawApplicationId, ApplicationId::of);
        return SecurityContext.currentPrincipal().flatMap(principal -> actor(principal).flatMap(actor -> owner.execute(applicationId)
                .map(tenant -> new AdministrationRequest(tenant, applicationId, actor, principal.subject(), Set.of(), principal.authenticationContext()))));
    }

    private PageWindow window(ListApplicationSecurityRelationRawRequest raw) {
        return ApplicationSecurityPageRequestMapper.toWindow(new ApplicationSecurityPageRawRequest(raw.page(), raw.size(), raw.offset(), raw.limit()));
    }

    private Mono<PageResponse<ApplicationSecurityRoleAssignmentWebResponse>> rolePage(ResultPage<AssignmentResponse> page,
                                                                                         AdministrationRequest administration) {
        return Flux.fromIterable(page.content()).concatMap(assignment -> Mono.zip(users.execute(assignment.userId()),
                roles.execute(new FindRoleForTenantRequest(assignment.roleId(), administration.tenantId())))
                .filter(pair -> pair.getT1().tenantId().equals(administration.tenantId()))
                .map(pair -> new ApplicationSecurityRoleAssignmentWebResponse(assignment.id().value().toString(), user(pair.getT1()),
                        new ApplicationSecurityRoleWebResponse(pair.getT2().id().value().toString(), pair.getT2().name().value(), pair.getT2().resources().size(), pair.getT2().registeredAt().toString()),
                        assignment.validFrom().toString(), assignment.validUntil().map(Object::toString).orElse(null)))).collectList()
                .map(content -> page(content, page));
    }

    private Mono<PageResponse<ApplicationSecurityProfileAssignmentWebResponse>> profilePage(ResultPage<ProfileAssignmentResponse> page,
                                                                                               AdministrationRequest administration) {
        return Flux.fromIterable(page.content()).concatMap(assignment -> Mono.zip(users.execute(assignment.userId()),
                profiles.execute(new FindProfileForTenantRequest(assignment.profileId(), administration.tenantId())))
                .filter(pair -> pair.getT1().tenantId().equals(administration.tenantId()))
                .map(pair -> new ApplicationSecurityProfileAssignmentWebResponse(assignment.id().value().toString(), user(pair.getT1()),
                        new ApplicationSecurityProfileWebResponse(pair.getT2().id().value().toString(), pair.getT2().name().value(), pair.getT2().roles().size(), pair.getT2().registeredAt().toString()),
                        assignment.validFrom().toString(), assignment.validUntil().map(Object::toString).orElse(null)))).collectList()
                .map(content -> page(content, page));
    }

    private static <T> PageResponse<T> page(java.util.List<T> content, ResultPage<?> source) {
        return new PageResponse<>(content, source.total(), source.window().page(), source.window().offset(), source.window().limit());
    }

    private static ApplicationSecurityUserWebResponse user(co.edu.uco.seguridad.pdp.identity.application.primaryport.response.UserResponse user) {
        return new ApplicationSecurityUserWebResponse(user.id().value().toString(), user.name(), user.email());
    }

    private Mono<UserId> actor(PdpPrincipal principal) {
        return principal.userId().map(Mono::just).orElseGet(() -> subjects.execute(principal.subject()));
    }
}
