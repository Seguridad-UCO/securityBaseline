package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor.impl;

import co.edu.uco.seguridad.pdp.applications.application.rule.validator.ApplicationOwnerLookupValidator;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AdministrationRequest;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.ListRoleResourcesRequest;
import co.edu.uco.seguridad.pdp.authorization.application.usecase.ListRoleResourcesUseCase;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.request.raw.ApplicationSecurityPageRawRequest;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.request.raw.ListApplicationSecurityRelationRawRequest;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.response.ApplicationSecurityResourceWebResponse;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor.ListRoleResourcesInteractor;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.mapper.ApplicationSecurityPageRequestMapper;
import co.edu.uco.seguridad.pdp.commons.model.*; import co.edu.uco.seguridad.pdp.identity.application.rule.validator.SubjectUserIdLookupValidator;
import co.edu.uco.seguridad.shared.security.*; import co.edu.uco.seguridad.shared.web.*; import reactor.core.publisher.Mono; import java.util.Set;

public final class ListRoleResourcesInteractorImpl implements ListRoleResourcesInteractor {
 private final ApplicationOwnerLookupValidator owner; private final SubjectUserIdLookupValidator ids; private final ListRoleResourcesUseCase useCase;
 public ListRoleResourcesInteractorImpl(ApplicationOwnerLookupValidator owner, SubjectUserIdLookupValidator ids, ListRoleResourcesUseCase useCase) { this.owner=owner; this.ids=ids; this.useCase=useCase; }
 public Mono<PageResponse<ApplicationSecurityResourceWebResponse>> execute(ListApplicationSecurityRelationRawRequest r) { ApplicationId app=RequestFieldParser.parse("applicationId",r.applicationId(),ApplicationId::of); RoleId role=RequestFieldParser.parse("roleId",r.entityId(),RoleId::of); PageWindow w=ApplicationSecurityPageRequestMapper.toWindow(new ApplicationSecurityPageRawRequest(r.page(),r.size(),r.offset(),r.limit())); return SecurityContext.currentPrincipal().flatMap(p->user(p).flatMap(u->owner.execute(app).map(t->new AdministrationRequest(t,app,u,p.subject(),Set.of(),p.authenticationContext())))).flatMap(a->useCase.execute(new ListRoleResourcesRequest(a,role,w))).map(p->new PageResponse<>(p.content().stream().map(x->new ApplicationSecurityResourceWebResponse(x.id().value().toString(),x.path().value(),x.method().name(),x.registeredAt().toString())).toList(),p.total(),p.window().page(),p.window().offset(),p.window().limit())); }
 private Mono<UserId> user(PdpPrincipal p) { return p.userId().map(Mono::just).orElseGet(()->ids.execute(p.subject())); }
}
