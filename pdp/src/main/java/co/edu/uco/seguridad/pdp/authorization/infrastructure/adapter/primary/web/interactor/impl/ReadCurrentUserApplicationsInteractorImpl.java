package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor.impl;

import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.ReadCurrentUserApplicationsRequest;
import co.edu.uco.seguridad.pdp.authorization.application.usecase.ReadCurrentUserApplicationsUseCase;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.response.CurrentUserApplicationsWebResponse;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.interactor.ReadCurrentUserApplicationsInteractor;
import co.edu.uco.seguridad.pdp.identity.application.usecase.FindAuthenticatedUserBySubjectUseCase;
import co.edu.uco.seguridad.shared.security.SecurityContext;
import org.springframework.security.access.AccessDeniedException;
import reactor.core.publisher.Mono;

import java.util.Objects;

public final class ReadCurrentUserApplicationsInteractorImpl implements ReadCurrentUserApplicationsInteractor {
    private final FindAuthenticatedUserBySubjectUseCase users;
    private final ReadCurrentUserApplicationsUseCase useCase;

    public ReadCurrentUserApplicationsInteractorImpl(FindAuthenticatedUserBySubjectUseCase users, ReadCurrentUserApplicationsUseCase useCase) {
        this.users = Objects.requireNonNull(users); this.useCase = Objects.requireNonNull(useCase);
    }

    @Override public Mono<CurrentUserApplicationsWebResponse> execute() {
        return SecurityContext.currentPrincipal().flatMap(principal -> users.execute(principal.subject())
                .switchIfEmpty(Mono.error(new AccessDeniedException("No local user for authenticated subject")))
                .filter(user -> user.tenantId().equals(principal.tenantId()))
                .switchIfEmpty(Mono.error(new AccessDeniedException("Authenticated subject belongs to another tenant")))
                .flatMap(user -> useCase.execute(new ReadCurrentUserApplicationsRequest(user.userId(), principal.tenantId(), user.name(), user.email()))))
                .map(response -> new CurrentUserApplicationsWebResponse(
                        new CurrentUserApplicationsWebResponse.Tenant(response.tenantName()),
                        new CurrentUserApplicationsWebResponse.User(response.userName(), response.email()),
                        response.applications().stream().map(app -> new CurrentUserApplicationsWebResponse.Application(
                                app.id().value().toString(), app.name(),
                                app.tenantRoles().stream().map(role -> new CurrentUserApplicationsWebResponse.NamedRole(role.name())).toList(),
                                app.applicationRoles().stream().map(role -> new CurrentUserApplicationsWebResponse.NamedRole(role.name())).toList(),
                                app.profiles().stream().map(profile -> new CurrentUserApplicationsWebResponse.Profile(profile.name(),
                                        profile.roles().stream().map(role -> new CurrentUserApplicationsWebResponse.ProfileRole(role.name(), role.scope())).toList())).toList())).toList()));
    }
}
