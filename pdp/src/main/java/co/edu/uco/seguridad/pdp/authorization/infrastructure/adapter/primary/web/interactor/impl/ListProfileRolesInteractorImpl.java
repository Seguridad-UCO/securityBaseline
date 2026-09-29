package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor.impl;

import co.edu.uco.seguridad.pdp.applications.application.rule.validator.ApplicationOwnerLookupValidator;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AdministrationRequest;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.ListProfileRolesRequest;
import co.edu.uco.seguridad.pdp.authorization.application.usecase.ListProfileRolesUseCase;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.request.raw.ApplicationSecurityPageRawRequest;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.request.raw.ListApplicationSecurityRelationRawRequest;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.response.ApplicationSecurityRoleWebResponse;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor.ListProfileRolesInteractor;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.mapper.ApplicationSecurityPageRequestMapper;
import co.edu.uco.seguridad.pdp.commons.model.*; import co.edu.uco.seguridad.pdp.identity.application.rule.validator.SubjectUserIdLookupValidator;
import co.edu.uco.seguridad.shared.security.*; import co.edu.uco.seguridad.shared.web.*; import reactor.core.publisher.Mono; import java.util.Set;

public final class ListProfileRolesInteractorImpl implements ListProfileRolesInteractor {
 private final ApplicationOwnerLookupValidator owner; private final SubjectUserIdLookupValidator ids; private final ListProfileRolesUseCase useCase;
 public ListProfileRolesInteractorImpl(ApplicationOwnerLookupValidator owner, SubjectUserIdLookupValidator ids, ListProfileRolesUseCase useCase) { this.owner=owner; this.ids=ids; this.useCase=useCase; }
 public Mono<PageResponse<ApplicationSecurityRoleWebResponse>> execute(ListApplicationSecurityRelationRawRequest r) { ApplicationId app=RequestFieldParser.parse("applicationId",r.applicationId(),ApplicationId::of); ProfileId profile=RequestFieldParser.parse("profileId",r.entityId(),ProfileId::of); PageWindow w=ApplicationSecurityPageRequestMapper.toWindow(new ApplicationSecurityPageRawRequest(r.page(),r.size(),r.offset(),r.limit())); return SecurityContext.currentPrincipal().flatMap(p->user(p).flatMap(u->owner.execute(app).map(t->new AdministrationRequest(t,app,u,p.subject(),Set.of(),p.authenticationContext())))).flatMap(a->useCase.execute(new ListProfileRolesRequest(a,profile,w))).map(p->new PageResponse<>(p.content().stream().map(x->new ApplicationSecurityRoleWebResponse(x.id().value().toString(),x.name().value(),x.resources().size(),x.registeredAt().toString())).toList(),p.total(),p.window().page(),p.window().offset(),p.window().limit())); }
 private Mono<UserId> user(PdpPrincipal p) { return p.userId().map(Mono::just).orElseGet(()->ids.execute(p.subject())); }
}
