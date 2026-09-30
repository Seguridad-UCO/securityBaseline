package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor.impl;

import co.edu.uco.seguridad.pdp.applications.application.rule.validator.ApplicationOwnerLookupValidator;
import co.edu.uco.seguridad.pdp.assignments.application.secondaryport.repository.*;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AdministrationRequest;
import co.edu.uco.seguridad.pdp.authorization.application.rule.validator.PrincipalMustBeApplicationAdministratorValidator;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.request.raw.*;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.response.*;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor.AssignmentDetailReadInteractor;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.mapper.ApplicationSecurityPageRequestMapper;
import co.edu.uco.seguridad.pdp.commons.model.*;
import co.edu.uco.seguridad.pdp.identity.application.rule.validator.SubjectUserIdLookupValidator;
import co.edu.uco.seguridad.pdp.identity.application.secondaryport.repository.SecurityUserRepository;
import co.edu.uco.seguridad.pdp.profiles.application.secondaryport.repository.ProfileRepository;
import co.edu.uco.seguridad.pdp.roles.application.secondaryport.repository.RoleRepository;
import co.edu.uco.seguridad.shared.port.TimeProvider;
import co.edu.uco.seguridad.shared.security.*;
import co.edu.uco.seguridad.shared.web.*;
import reactor.core.publisher.*;
import java.util.Set;

/** Reads only relationships inside an application after the PDP administration decision. */
public final class AssignmentDetailReadInteractorImpl implements AssignmentDetailReadInteractor {
 private final ApplicationOwnerLookupValidator owner; private final SubjectUserIdLookupValidator subjects; private final PrincipalMustBeApplicationAdministratorValidator gate; private final AssignmentRepository rolesAssignments; private final ProfileAssignmentRepository profilesAssignments; private final SecurityUserRepository users; private final RoleRepository roles; private final ProfileRepository profiles; private final TimeProvider clock;
 public AssignmentDetailReadInteractorImpl(ApplicationOwnerLookupValidator owner, SubjectUserIdLookupValidator subjects, PrincipalMustBeApplicationAdministratorValidator gate, AssignmentRepository rolesAssignments, ProfileAssignmentRepository profilesAssignments, SecurityUserRepository users, RoleRepository roles, ProfileRepository profiles, TimeProvider clock){this.owner=owner;this.subjects=subjects;this.gate=gate;this.rolesAssignments=rolesAssignments;this.profilesAssignments=profilesAssignments;this.users=users;this.roles=roles;this.profiles=profiles;this.clock=clock;}
 public Mono<PageResponse<ApplicationSecurityRoleAssignmentWebResponse>> roles(ListApplicationSecurityRelationRawRequest raw){return administration(raw.applicationId()).flatMap(a->{RoleId id=RequestFieldParser.parse("roleId",raw.entityId(),RoleId::of);PageWindow w=window(raw);return gate.execute(a).then(rolesAssignments.findActivePageByRoleAndApplication(id,a.tenantId(),a.applicationId(),clock.now(),w)).flatMap(page->rolePage(page,a));});}
 public Mono<PageResponse<ApplicationSecurityProfileAssignmentWebResponse>> profiles(ListApplicationSecurityRelationRawRequest raw){return administration(raw.applicationId()).flatMap(a->{ProfileId id=RequestFieldParser.parse("profileId",raw.entityId(),ProfileId::of);PageWindow w=window(raw);return gate.execute(a).then(profilesAssignments.findActivePageByProfileAndApplication(id,a.tenantId(),a.applicationId(),clock.now(),w)).flatMap(page->profilePage(page,a));});}
 public Mono<ApplicationSecurityUserAssignmentsWebResponse> user(ListApplicationSecurityRelationRawRequest raw){return administration(raw.applicationId()).flatMap(a->{UserId id=RequestFieldParser.parse("userId",raw.entityId(),UserId::of);PageWindow w=window(raw);return gate.execute(a).then(users.findById(id).filter(u->u.tenantId().equals(a.tenantId())).switchIfEmpty(Mono.error(new IllegalArgumentException("La persona no pertenece a esta aplicación"))).flatMap(u->Mono.zip(rolesAssignments.findActivePageByUserAndApplication(id,a.tenantId(),a.applicationId(),clock.now(),w).flatMap(p->rolePage(p,a)),profilesAssignments.findActivePageByUserAndApplication(id,a.tenantId(),a.applicationId(),clock.now(),w).flatMap(p->profilePage(p,a))).map(p->new ApplicationSecurityUserAssignmentsWebResponse(user(u),p.getT1(),p.getT2()))));});}
 private Mono<AdministrationRequest> administration(String raw){ApplicationId app=RequestFieldParser.parse("applicationId",raw,ApplicationId::of);return SecurityContext.currentPrincipal().flatMap(p->actor(p).flatMap(actor->owner.execute(app).map(tenant->new AdministrationRequest(tenant,app,actor,p.subject(),Set.of(),p.authenticationContext()))));}
 private PageWindow window(ListApplicationSecurityRelationRawRequest raw){return ApplicationSecurityPageRequestMapper.toWindow(new ApplicationSecurityPageRawRequest(raw.page(),raw.size(),raw.offset(),raw.limit()));}
 private Mono<PageResponse<ApplicationSecurityRoleAssignmentWebResponse>> rolePage(ResultPage<co.edu.uco.seguridad.pdp.assignments.domain.Assignment> page, AdministrationRequest a) {
   return Flux.fromIterable(page.content()).concatMap(x -> Mono.zip(users.findById(x.userId()), roles.findByIdForTenant(x.roleId(), a.tenantId()))
     .filter(pair -> pair.getT1().tenantId().equals(a.tenantId()))
     .map(pair -> new ApplicationSecurityRoleAssignmentWebResponse(x.id().value().toString(), user(pair.getT1()), new ApplicationSecurityRoleWebResponse(pair.getT2().id().value().toString(), pair.getT2().name().value(), pair.getT2().resources().size(), pair.getT2().registeredAt().toString()), x.validity().validFrom().toString(), x.validity().validUntil().map(Object::toString).orElse(null)))
   ).collectList().map(content -> new PageResponse<>(content, page.total(), page.window().page(), page.window().offset(), page.window().limit()));
 }
 private Mono<PageResponse<ApplicationSecurityProfileAssignmentWebResponse>> profilePage(ResultPage<co.edu.uco.seguridad.pdp.assignments.domain.ProfileAssignment> page, AdministrationRequest a) {
   return Flux.fromIterable(page.content()).concatMap(x -> Mono.zip(users.findById(x.userId()), profiles.findByIdForTenant(x.profileId(), a.tenantId()))
     .filter(pair -> pair.getT1().tenantId().equals(a.tenantId()))
     .map(pair -> new ApplicationSecurityProfileAssignmentWebResponse(x.id().value().toString(), user(pair.getT1()), new ApplicationSecurityProfileWebResponse(pair.getT2().id().value().toString(), pair.getT2().name().value(), pair.getT2().roles().size(), pair.getT2().registeredAt().toString()), x.validity().validFrom().toString(), x.validity().validUntil().map(Object::toString).orElse(null)))
   ).collectList().map(content -> new PageResponse<>(content, page.total(), page.window().page(), page.window().offset(), page.window().limit()));
 }
 private ApplicationSecurityUserWebResponse user(co.edu.uco.seguridad.pdp.identity.domain.SecurityUser u){return new ApplicationSecurityUserWebResponse(u.id().value().toString(),u.name(),u.email().value());}
 private Mono<UserId> actor(PdpPrincipal p){return p.userId().map(Mono::just).orElseGet(()->subjects.execute(p.subject()));}
}
