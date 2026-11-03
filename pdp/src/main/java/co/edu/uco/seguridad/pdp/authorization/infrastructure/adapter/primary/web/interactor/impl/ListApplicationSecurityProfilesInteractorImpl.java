package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor.impl;

import co.edu.uco.seguridad.pdp.applications.application.rule.validator.ApplicationOwnerLookupValidator;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.AdministrationRequest;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.ListApplicationSecurityProfilesRequest;
import co.edu.uco.seguridad.pdp.authorization.application.usecase.ListApplicationSecurityProfilesUseCase;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.request.raw.ApplicationSecurityPageRawRequest;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.request.raw.ListApplicationSecurityRawRequest;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.response.ApplicationSecurityProfileWebResponse;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor.ListApplicationSecurityProfilesInteractor;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.mapper.ApplicationSecurityPageRequestMapper;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.PageWindow;
import co.edu.uco.seguridad.pdp.commons.model.UserId;
import co.edu.uco.seguridad.pdp.identity.application.rule.validator.SubjectUserIdLookupValidator;
import co.edu.uco.seguridad.shared.security.PdpPrincipal;
import co.edu.uco.seguridad.shared.security.SecurityContext;
import co.edu.uco.seguridad.shared.web.PageResponse;
import co.edu.uco.seguridad.shared.web.RequestFieldParser;
import reactor.core.publisher.Mono;

import java.util.Set;

public final class ListApplicationSecurityProfilesInteractorImpl implements ListApplicationSecurityProfilesInteractor {
    private final ApplicationOwnerLookupValidator owner;
    private final SubjectUserIdLookupValidator ids;
    private final ListApplicationSecurityProfilesUseCase useCase;

    public ListApplicationSecurityProfilesInteractorImpl(ApplicationOwnerLookupValidator owner, SubjectUserIdLookupValidator ids, ListApplicationSecurityProfilesUseCase useCase) {
        this.owner = owner;
        this.ids = ids;
        this.useCase = useCase;
    }

    public Mono<PageResponse<ApplicationSecurityProfileWebResponse>> execute(ListApplicationSecurityRawRequest r) {
        ApplicationId app = RequestFieldParser.parse("applicationId", r.applicationId(), ApplicationId::of);
        PageWindow w = ApplicationSecurityPageRequestMapper.toWindow(new ApplicationSecurityPageRawRequest(r.page(), r.size(), r.offset(), r.limit()));
        return SecurityContext.currentPrincipal().flatMap(p -> user(p).flatMap(u -> owner.execute(app).map(t -> new AdministrationRequest(t, app, u, p.subject(), Set.of(), p.authenticationContext())))).flatMap(a -> useCase.execute(new ListApplicationSecurityProfilesRequest(a, w, r.query()))).map(p -> new PageResponse<>(p.content().stream().map(x -> new ApplicationSecurityProfileWebResponse(x.id().value().toString(), x.name().value(), x.roles().size(), x.registeredAt().toString())).toList(), p.total(), p.window().page(), p.window().offset(), p.window().limit()));
    }

    private Mono<UserId> user(PdpPrincipal p) {
        return p.userId().map(Mono::just).orElseGet(() -> ids.execute(p.subject()));
    }
}
