package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor.impl;

import co.edu.uco.seguridad.pdp.applications.application.rule.validator.ApplicationOwnerLookupValidator;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AdministrationRequest;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.ListApplicationRoleAssignmentsRequest;
import co.edu.uco.seguridad.pdp.authorization.application.usecase.ListApplicationRoleAssignmentsUseCase;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.request.raw.ApplicationSecurityPageRawRequest;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.request.raw.ListApplicationSecurityRawRequest;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.response.*;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor.ListApplicationRoleAssignmentsInteractor;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.mapper.ApplicationSecurityPageRequestMapper;
import co.edu.uco.seguridad.pdp.commons.model.*;
import co.edu.uco.seguridad.pdp.identity.application.rule.validator.SubjectUserIdLookupValidator;
import co.edu.uco.seguridad.pdp.identity.application.secondaryport.repository.SecurityUserRepository;
import co.edu.uco.seguridad.pdp.roles.application.secondaryport.repository.RoleRepository;
import co.edu.uco.seguridad.shared.security.*;
import co.edu.uco.seguridad.shared.web.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.util.Set;

public final class ListApplicationRoleAssignmentsInteractorImpl implements ListApplicationRoleAssignmentsInteractor {
  private final ApplicationOwnerLookupValidator owner; private final SubjectUserIdLookupValidator ids; private final ListApplicationRoleAssignmentsUseCase useCase; private final SecurityUserRepository users; private final RoleRepository roles;
  public ListApplicationRoleAssignmentsInteractorImpl(ApplicationOwnerLookupValidator owner, SubjectUserIdLookupValidator ids, ListApplicationRoleAssignmentsUseCase useCase, SecurityUserRepository users, RoleRepository roles) { this.owner=owner; this.ids=ids; this.useCase=useCase; this.users=users; this.roles=roles; }
  public Mono<PageResponse<ApplicationSecurityRoleAssignmentWebResponse>> execute(ListApplicationSecurityRawRequest raw) {
    ApplicationId app=RequestFieldParser.parse("applicationId",raw.applicationId(),ApplicationId::of);
    PageWindow window=ApplicationSecurityPageRequestMapper.toWindow(new ApplicationSecurityPageRawRequest(raw.page(),raw.size(),raw.offset(),raw.limit()));
    return SecurityContext.currentPrincipal().flatMap(principal -> user(principal).flatMap(actor -> owner.execute(app).map(tenant -> new AdministrationRequest(tenant,app,actor,principal.subject(),Set.of(),principal.authenticationContext()))))
      .flatMap(admin -> useCase.execute(new ListApplicationRoleAssignmentsRequest(admin,window)).flatMap(page -> Flux.fromIterable(page.content()).concatMap(assignment -> Mono.zip(users.findById(assignment.userId()),roles.findByIdForTenant(assignment.roleId(),admin.tenantId()))
        .filter(pair -> pair.getT1().tenantId().equals(admin.tenantId()))
        .map(pair -> new ApplicationSecurityRoleAssignmentWebResponse(assignment.id().value().toString(),new ApplicationSecurityUserWebResponse(pair.getT1().id().value().toString(),pair.getT1().name(),pair.getT1().email().value()),new ApplicationSecurityRoleWebResponse(pair.getT2().id().value().toString(),pair.getT2().name().value(),pair.getT2().resources().size(),pair.getT2().registeredAt().toString()),assignment.validFrom().toString(),assignment.validUntil().map(Object::toString).orElse(null)))).collectList().map(content -> new PageResponse<>(content,page.total(),page.window().page(),page.window().offset(),page.window().limit()))));
  }
  private Mono<UserId> user(PdpPrincipal principal) { return principal.userId().map(Mono::just).orElseGet(() -> ids.execute(principal.subject())); }
}
