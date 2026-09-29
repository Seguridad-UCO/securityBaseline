package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor.impl;
import co.edu.uco.seguridad.pdp.applications.application.rule.validator.ApplicationOwnerLookupValidator;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.*;
import co.edu.uco.seguridad.pdp.authorization.application.usecase.ListApplicationSecurityAdministratorsUseCase;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.request.raw.*;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.response.*;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor.ListApplicationSecurityAdministratorsInteractor;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.mapper.ApplicationSecurityPageRequestMapper;
import co.edu.uco.seguridad.pdp.commons.model.*;
import co.edu.uco.seguridad.pdp.identity.application.rule.validator.SubjectUserIdLookupValidator;
import co.edu.uco.seguridad.pdp.identity.application.secondaryport.repository.SecurityUserRepository;
import co.edu.uco.seguridad.shared.security.*; import co.edu.uco.seguridad.shared.web.*; import reactor.core.publisher.*; import java.util.Set;
public final class ListApplicationSecurityAdministratorsInteractorImpl implements ListApplicationSecurityAdministratorsInteractor {
 private final ApplicationOwnerLookupValidator owner; private final SubjectUserIdLookupValidator ids; private final ListApplicationSecurityAdministratorsUseCase useCase; private final SecurityUserRepository users;
 public ListApplicationSecurityAdministratorsInteractorImpl(ApplicationOwnerLookupValidator owner, SubjectUserIdLookupValidator ids, ListApplicationSecurityAdministratorsUseCase useCase, SecurityUserRepository users){this.owner=owner;this.ids=ids;this.useCase=useCase;this.users=users;}
 public Mono<PageResponse<ApplicationAdministratorWebResponse>> execute(ListApplicationSecurityRawRequest raw){ApplicationId app=RequestFieldParser.parse("applicationId",raw.applicationId(),ApplicationId::of);PageWindow w=ApplicationSecurityPageRequestMapper.toWindow(new ApplicationSecurityPageRawRequest(raw.page(),raw.size(),raw.offset(),raw.limit()));return SecurityContext.currentPrincipal().flatMap(p->user(p).flatMap(actor->owner.execute(app).map(tenant->new AdministrationRequest(tenant,app,actor,p.subject(),Set.of(),p.authenticationContext())))).flatMap(admin->useCase.execute(new ListApplicationSecurityAdministratorsRequest(admin,w)).flatMap(page->Flux.fromIterable(page.content()).concatMap(item->users.findById(item.userId()).filter(u->u.tenantId().equals(admin.tenantId())).map(u->new ApplicationAdministratorWebResponse(new ApplicationSecurityUserWebResponse(u.id().value().toString(),u.name(),u.email().value()),item.validFrom().toString(),item.validUntil().map(Object::toString).orElse(null)))).collectList().map(content->new PageResponse<>(content,page.total(),page.window().page(),page.window().offset(),page.window().limit()))));}
 private Mono<UserId> user(PdpPrincipal p){return p.userId().map(Mono::just).orElseGet(()->ids.execute(p.subject()));}
}
