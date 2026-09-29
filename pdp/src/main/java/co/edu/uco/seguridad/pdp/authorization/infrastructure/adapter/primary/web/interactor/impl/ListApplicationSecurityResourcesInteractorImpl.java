package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor.impl;
import co.edu.uco.seguridad.pdp.applications.application.rule.validator.ApplicationOwnerLookupValidator;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AdministrationRequest;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.ListApplicationSecurityResourcesRequest;
import co.edu.uco.seguridad.pdp.authorization.application.usecase.ListApplicationSecurityResourcesUseCase;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.request.raw.*;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.response.ApplicationSecurityResourceWebResponse;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor.ListApplicationSecurityResourcesInteractor;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.mapper.ApplicationSecurityPageRequestMapper;
import co.edu.uco.seguridad.pdp.commons.model.*;
import co.edu.uco.seguridad.pdp.identity.application.rule.validator.SubjectUserIdLookupValidator;
import co.edu.uco.seguridad.shared.security.*;
import co.edu.uco.seguridad.shared.web.*;
import reactor.core.publisher.Mono;
import java.util.Set;
public final class ListApplicationSecurityResourcesInteractorImpl implements ListApplicationSecurityResourcesInteractor {
 private final ApplicationOwnerLookupValidator owner; private final SubjectUserIdLookupValidator identities; private final ListApplicationSecurityResourcesUseCase useCase;
 public ListApplicationSecurityResourcesInteractorImpl(ApplicationOwnerLookupValidator owner,SubjectUserIdLookupValidator identities,ListApplicationSecurityResourcesUseCase useCase){this.owner=owner;this.identities=identities;this.useCase=useCase;}
 public Mono<PageResponse<ApplicationSecurityResourceWebResponse>> execute(ListApplicationSecurityRawRequest raw){ ApplicationId app=RequestFieldParser.parse("applicationId",raw.applicationId(),ApplicationId::of); PageWindow w=ApplicationSecurityPageRequestMapper.toWindow(new ApplicationSecurityPageRawRequest(raw.page(),raw.size(),raw.offset(),raw.limit())); return SecurityContext.currentPrincipal().flatMap(p->user(p).flatMap(u->owner.execute(app).map(t->new AdministrationRequest(t,app,u,p.subject(),Set.of(),p.authenticationContext())))).flatMap(a->useCase.execute(new ListApplicationSecurityResourcesRequest(a,w))).map(p->new PageResponse<>(p.content().stream().map(r->new ApplicationSecurityResourceWebResponse(r.id().value().toString(),r.path().value(),r.method().name(),r.registeredAt().toString())).toList(),p.total(),p.window().page(),p.window().offset(),p.window().limit())); }
 private Mono<UserId> user(PdpPrincipal p){return p.userId().map(Mono::just).orElseGet(()->identities.execute(p.subject()));}
}
