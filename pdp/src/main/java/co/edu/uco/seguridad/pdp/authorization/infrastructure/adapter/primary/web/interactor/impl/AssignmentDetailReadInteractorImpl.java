package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor.impl;

import co.edu.uco.seguridad.pdp.applications.application.rule.validator.ApplicationOwnerLookupValidator;
import co.edu.uco.seguridad.pdp.assignments.application.primaryport.request.ListActiveAssignmentsPageRequest;
import co.edu.uco.seguridad.pdp.assignments.application.primaryport.response.AssignmentResponse;
import co.edu.uco.seguridad.pdp.assignments.application.primaryport.response.ProfileAssignmentResponse;
import co.edu.uco.seguridad.pdp.assignments.application.usecase.ListActiveProfileAssignmentsByProfilePageUseCase;
import co.edu.uco.seguridad.pdp.assignments.application.usecase.ListActiveProfileAssignmentsByUserPageUseCase;
import co.edu.uco.seguridad.pdp.assignments.application.usecase.ListActiveRoleAssignmentsByRolePageUseCase;
import co.edu.uco.seguridad.pdp.assignments.application.usecase.ListActiveRoleAssignmentsByUserPageUseCase;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AdministrationRequest;
import co.edu.uco.seguridad.pdp.authorization.application.rule.validator.PrincipalMustBeApplicationAdministratorValidator;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.request.raw.*;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.response.*;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor.AssignmentDetailReadInteractor;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.mapper.ApplicationSecurityPageRequestMapper;
import co.edu.uco.seguridad.pdp.commons.model.*;
import co.edu.uco.seguridad.pdp.identity.application.primaryport.response.UserResponse;
import co.edu.uco.seguridad.pdp.identity.application.rule.validator.SubjectUserIdLookupValidator;
import co.edu.uco.seguridad.pdp.identity.application.usecase.FindUserByIdUseCase;
import co.edu.uco.seguridad.pdp.profiles.application.secondaryport.repository.ProfileRepository;
import co.edu.uco.seguridad.pdp.roles.application.secondaryport.repository.RoleRepository;
import co.edu.uco.seguridad.shared.security.*;
import co.edu.uco.seguridad.shared.web.*;
import reactor.core.publisher.*;
import java.util.Set;

/** Reads only relationships inside an application after the PDP administration decision. */
public final class AssignmentDetailReadInteractorImpl implements AssignmentDetailReadInteractor {

    private final ApplicationOwnerLookupValidator owner;
    private final SubjectUserIdLookupValidator subjects;
    private final PrincipalMustBeApplicationAdministratorValidator gate;
    private final ListActiveRoleAssignmentsByRolePageUseCase roleAssignmentsByRole;
    private final ListActiveRoleAssignmentsByUserPageUseCase roleAssignmentsByUser;
    private final ListActiveProfileAssignmentsByProfilePageUseCase profileAssignmentsByProfile;
    private final ListActiveProfileAssignmentsByUserPageUseCase profileAssignmentsByUser;
    private final FindUserByIdUseCase users;
    private final RoleRepository roles;
    private final ProfileRepository profiles;

    public AssignmentDetailReadInteractorImpl(ApplicationOwnerLookupValidator owner,
            SubjectUserIdLookupValidator subjects, PrincipalMustBeApplicationAdministratorValidator gate,
            ListActiveRoleAssignmentsByRolePageUseCase roleAssignmentsByRole,
            ListActiveRoleAssignmentsByUserPageUseCase roleAssignmentsByUser,
            ListActiveProfileAssignmentsByProfilePageUseCase profileAssignmentsByProfile,
            ListActiveProfileAssignmentsByUserPageUseCase profileAssignmentsByUser,
            FindUserByIdUseCase users, RoleRepository roles, ProfileRepository profiles) {
        this.owner = owner;
        this.subjects = subjects;
        this.gate = gate;
        this.roleAssignmentsByRole = roleAssignmentsByRole;
        this.roleAssignmentsByUser = roleAssignmentsByUser;
        this.profileAssignmentsByProfile = profileAssignmentsByProfile;
        this.profileAssignmentsByUser = profileAssignmentsByUser;
        this.users = users;
        this.roles = roles;
        this.profiles = profiles;
    }

    public Mono<PageResponse<ApplicationSecurityRoleAssignmentWebResponse>> roles(ListApplicationSecurityRelationRawRequest raw) {
        return administration(raw.applicationId()).flatMap(a -> {
            RoleId id = RequestFieldParser.parse("roleId", raw.entityId(), RoleId::of);
            PageWindow w = window(raw);
            return gate.execute(a)
                    .then(roleAssignmentsByRole.execute(new ListActiveAssignmentsPageRequest<>(id, a.tenantId(), a.applicationId(), w)))
                    .flatMap(page -> rolePage(page, a));
        });
    }

    public Mono<PageResponse<ApplicationSecurityProfileAssignmentWebResponse>> profiles(ListApplicationSecurityRelationRawRequest raw) {
        return administration(raw.applicationId()).flatMap(a -> {
            ProfileId id = RequestFieldParser.parse("profileId", raw.entityId(), ProfileId::of);
            PageWindow w = window(raw);
            return gate.execute(a)
                    .then(profileAssignmentsByProfile.execute(new ListActiveAssignmentsPageRequest<>(id, a.tenantId(), a.applicationId(), w)))
                    .flatMap(page -> profilePage(page, a));
        });
    }

    public Mono<ApplicationSecurityUserAssignmentsWebResponse> user(ListApplicationSecurityRelationRawRequest raw) {
        return administration(raw.applicationId()).flatMap(a -> {
            UserId id = RequestFieldParser.parse("userId", raw.entityId(), UserId::of);
            PageWindow w = window(raw);
            ListActiveAssignmentsPageRequest<UserId> query =
                    new ListActiveAssignmentsPageRequest<>(id, a.tenantId(), a.applicationId(), w);
            return gate.execute(a).then(users.execute(id)
                    .filter(u -> u.tenantId().equals(a.tenantId()))
                    .switchIfEmpty(Mono.error(new IllegalArgumentException("La persona no pertenece a esta aplicación")))
                    .flatMap(u -> Mono.zip(
                                    roleAssignmentsByUser.execute(query).flatMap(p -> rolePage(p, a)),
                                    profileAssignmentsByUser.execute(query).flatMap(p -> profilePage(p, a)))
                            .map(p -> new ApplicationSecurityUserAssignmentsWebResponse(user(u), p.getT1(), p.getT2()))));
        });
    }

    private Mono<AdministrationRequest> administration(String raw) {
        ApplicationId app = RequestFieldParser.parse("applicationId", raw, ApplicationId::of);
        return SecurityContext.currentPrincipal().flatMap(p -> actor(p).flatMap(actor -> owner.execute(app)
                .map(tenant -> new AdministrationRequest(tenant, app, actor, p.subject(), Set.of(), p.authenticationContext()))));
    }

    private PageWindow window(ListApplicationSecurityRelationRawRequest raw) {
        return ApplicationSecurityPageRequestMapper.toWindow(
                new ApplicationSecurityPageRawRequest(raw.page(), raw.size(), raw.offset(), raw.limit()));
    }

    private Mono<PageResponse<ApplicationSecurityRoleAssignmentWebResponse>> rolePage(
            ResultPage<AssignmentResponse> page, AdministrationRequest a) {
        return Flux.fromIterable(page.content())
                .concatMap(x -> Mono.zip(users.execute(x.userId()), roles.findByIdForTenant(x.roleId(), a.tenantId()))
                        .filter(pair -> pair.getT1().tenantId().equals(a.tenantId()))
                        .map(pair -> new ApplicationSecurityRoleAssignmentWebResponse(x.id().value().toString(),
                                user(pair.getT1()),
                                new ApplicationSecurityRoleWebResponse(pair.getT2().id().value().toString(),
                                        pair.getT2().name().value(), pair.getT2().resources().size(),
                                        pair.getT2().registeredAt().toString()),
                                x.validFrom().toString(), x.validUntil().map(Object::toString).orElse(null))))
                .collectList()
                .map(content -> new PageResponse<>(content, page.total(), page.window().page(),
                        page.window().offset(), page.window().limit()));
    }

    private Mono<PageResponse<ApplicationSecurityProfileAssignmentWebResponse>> profilePage(
            ResultPage<ProfileAssignmentResponse> page, AdministrationRequest a) {
        return Flux.fromIterable(page.content())
                .concatMap(x -> Mono.zip(users.execute(x.userId()), profiles.findByIdForTenant(x.profileId(), a.tenantId()))
                        .filter(pair -> pair.getT1().tenantId().equals(a.tenantId()))
                        .map(pair -> new ApplicationSecurityProfileAssignmentWebResponse(x.id().value().toString(),
                                user(pair.getT1()),
                                new ApplicationSecurityProfileWebResponse(pair.getT2().id().value().toString(),
                                        pair.getT2().name().value(), pair.getT2().roles().size(),
                                        pair.getT2().registeredAt().toString()),
                                x.validFrom().toString(), x.validUntil().map(Object::toString).orElse(null))))
                .collectList()
                .map(content -> new PageResponse<>(content, page.total(), page.window().page(),
                        page.window().offset(), page.window().limit()));
    }

    private ApplicationSecurityUserWebResponse user(UserResponse u) {
        return new ApplicationSecurityUserWebResponse(u.id().value().toString(), u.name(), u.email());
    }

    private Mono<UserId> actor(PdpPrincipal p) {
        return p.userId().map(Mono::just).orElseGet(() -> subjects.execute(p.subject()));
    }
}
