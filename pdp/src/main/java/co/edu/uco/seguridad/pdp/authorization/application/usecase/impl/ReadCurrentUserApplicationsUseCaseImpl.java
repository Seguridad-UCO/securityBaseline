package co.edu.uco.seguridad.pdp.authorization.application.usecase.impl;

import co.edu.uco.seguridad.pdp.applications.application.rule.validator.ApplicationDetailsLookupValidator;
import co.edu.uco.seguridad.pdp.assignments.application.primaryport.request.ResolveUserApplicationsRequest;
import co.edu.uco.seguridad.pdp.assignments.application.usecase.ResolveUserApplicationsUseCase;
import co.edu.uco.seguridad.pdp.authorization.domain.exception.CurrentUserApplicationsAccessDeniedException;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.ReadCurrentUserApplicationsRequest;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.response.CurrentUserApplicationsResponse;
import co.edu.uco.seguridad.pdp.authorization.application.usecase.ReadCurrentUserApplicationsUseCase;
import co.edu.uco.seguridad.pdp.roles.application.secondaryport.repository.RoleRepository;
import co.edu.uco.seguridad.pdp.tenants.application.usecase.FindTenantNameByCodeUseCase;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Comparator;
import java.util.Objects;

public final class ReadCurrentUserApplicationsUseCaseImpl implements ReadCurrentUserApplicationsUseCase {
    private final ResolveUserApplicationsUseCase assignments;
    private final FindTenantNameByCodeUseCase tenants;
    private final ApplicationDetailsLookupValidator applications;
    private final RoleRepository roles;

    public ReadCurrentUserApplicationsUseCaseImpl(ResolveUserApplicationsUseCase assignments, FindTenantNameByCodeUseCase tenants,
            ApplicationDetailsLookupValidator applications, RoleRepository roles) {
        this.assignments = Objects.requireNonNull(assignments); this.tenants = Objects.requireNonNull(tenants);
        this.applications = Objects.requireNonNull(applications); this.roles = Objects.requireNonNull(roles);
    }

    @Override public Mono<CurrentUserApplicationsResponse> execute(ReadCurrentUserApplicationsRequest input) {
        return tenants.execute(input.tenantId()).switchIfEmpty(denied())
                .flatMap(tenant -> assignments.execute(new ResolveUserApplicationsRequest(input.userId(), input.tenantId()))
                        .flatMapMany(all -> Flux.fromIterable(all.rolesByApplication().entrySet()))
                        .flatMap(entry -> applications.execute(entry.getKey())
                                .filter(application -> application.tenantId().equals(input.tenantId()))
                                .switchIfEmpty(denied())
                                .flatMap(application -> Flux.fromIterable(entry.getValue())
                                        .flatMap(roleId -> roles.findById(roleId)
                                                .filter(role -> role.scope().isGlobal()
                                                        || role.scope().tenantId().filter(input.tenantId()::equals).isPresent())
                                                .switchIfEmpty(denied()))
                                        .map(role -> role.name().value())
                                        .sort()
                                        .collectList()
                                        .map(roleNames -> new CurrentUserApplicationsResponse.ApplicationRoles(application.name().value(), roleNames))))
                        .sort(Comparator.comparing(CurrentUserApplicationsResponse.ApplicationRoles::name))
                        .collectList()
                        .map(apps -> new CurrentUserApplicationsResponse(tenant.name(), input.userName(), input.email(), apps)));
    }

    private static <T> Mono<T> denied() {
        return Mono.error(new CurrentUserApplicationsAccessDeniedException());
    }
}
