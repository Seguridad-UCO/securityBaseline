package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor.impl;

import co.edu.uco.seguridad.pdp.applications.application.rule.validator.ApplicationOwnerLookupValidator;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.*;
import co.edu.uco.seguridad.pdp.authorization.application.usecase.ListApplicationProfileAssignmentsUseCase;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.request.raw.*;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.response.*;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor.ListApplicationProfileAssignmentsInteractor;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.mapper.ApplicationSecurityPageRequestMapper;
import co.edu.uco.seguridad.pdp.commons.model.*;
import co.edu.uco.seguridad.pdp.identity.application.rule.validator.SubjectUserIdLookupValidator;
import co.edu.uco.seguridad.pdp.identity.application.secondaryport.repository.SecurityUserRepository;
import co.edu.uco.seguridad.pdp.profiles.application.secondaryport.repository.ProfileRepository;
import co.edu.uco.seguridad.shared.security.*;
import co.edu.uco.seguridad.shared.web.*;
import reactor.core.publisher.*;
import java.util.Set;

public final class ListApplicationProfileAssignmentsInteractorImpl implements ListApplicationProfileAssignmentsInteractor {
  private final ApplicationOwnerLookupValidator owner; private final SubjectUserIdLookupValidator ids; private final ListApplicationProfileAssignmentsUseCase useCase; private final SecurityUserRepository users; private final ProfileRepository profiles;
  public ListApplicationProfileAssignmentsInteractorImpl(ApplicationOwnerLookupValidator owner, SubjectUserIdLookupValidator ids, ListApplicationProfileAssignmentsUseCase useCase, SecurityUserRepository users, ProfileRepository profiles) { this.owner=owner;this.ids=ids;this.useCase=useCase;this.users=users;this.profiles=profiles; }
  public Mono<PageResponse<ApplicationSecurityProfileAssignmentWebResponse>> execute(ListApplicationSecurityRawRequest raw) {
    ApplicationId app=RequestFieldParser.parse("applicationId",raw.applicationId(),ApplicationId::of); PageWindow window=ApplicationSecurityPageRequestMapper.toWindow(new ApplicationSecurityPageRawRequest(raw.page(),raw.size(),raw.offset(),raw.limit()));
    return SecurityContext.currentPrincipal().flatMap(p -> user(p).flatMap(actor -> owner.execute(app).map(tenant -> new AdministrationRequest(tenant,app,actor,p.subject(),Set.of(),p.authenticationContext()))))
      .flatMap(admin -> useCase.execute(new ListApplicationProfileAssignmentsRequest(admin,window)).flatMap(page -> Flux.fromIterable(page.content()).concatMap(assignment -> Mono.zip(users.findById(assignment.userId()),profiles.findByIdForTenant(assignment.profileId(),admin.tenantId()))
        .filter(pair -> pair.getT1().tenantId().equals(admin.tenantId()))
        .map(pair -> new ApplicationSecurityProfileAssignmentWebResponse(assignment.id().value().toString(),new ApplicationSecurityUserWebResponse(pair.getT1().id().value().toString(),pair.getT1().name(),pair.getT1().email().value()),new ApplicationSecurityProfileWebResponse(pair.getT2().id().value().toString(),pair.getT2().name().value(),pair.getT2().roles().size(),pair.getT2().registeredAt().toString()),assignment.validFrom().toString(),assignment.validUntil().map(Object::toString).orElse(null)))).collectList().map(content -> new PageResponse<>(content,page.total(),page.window().page(),page.window().offset(),page.window().limit()))));
  }
  private Mono<UserId> user(PdpPrincipal principal) { return principal.userId().map(Mono::just).orElseGet(() -> ids.execute(principal.subject())); }
}
