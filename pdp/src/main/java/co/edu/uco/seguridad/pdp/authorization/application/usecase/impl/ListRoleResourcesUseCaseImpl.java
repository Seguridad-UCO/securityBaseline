package co.edu.uco.seguridad.pdp.authorization.application.usecase.impl;

import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.ListRoleResourcesRequest;
import co.edu.uco.seguridad.pdp.authorization.application.rule.validator.PrincipalMustBeApplicationAdministratorValidator;
import co.edu.uco.seguridad.pdp.authorization.application.usecase.ListRoleResourcesUseCase;
import co.edu.uco.seguridad.pdp.commons.model.ResultPage;
import co.edu.uco.seguridad.pdp.resources.application.primaryport.response.RegisteredProtectedResourceResponse;
import co.edu.uco.seguridad.pdp.resources.application.secondaryport.repository.ProtectedResourceRepository;
import co.edu.uco.seguridad.pdp.roles.application.secondaryport.repository.RoleRepository;
import co.edu.uco.seguridad.pdp.roles.domain.exception.RoleNotFoundException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Locale;

/**
 * Does pagination before materialising resources; the browser never receives a role-wide catalog.
 */
public final class ListRoleResourcesUseCaseImpl implements ListRoleResourcesUseCase {
    private final PrincipalMustBeApplicationAdministratorValidator gate;
    private final RoleRepository roles;
    private final ProtectedResourceRepository resources;

    public ListRoleResourcesUseCaseImpl(PrincipalMustBeApplicationAdministratorValidator gate, RoleRepository roles, ProtectedResourceRepository resources) {
        this.gate = gate;
        this.roles = roles;
        this.resources = resources;
    }

    @Override
    public Mono<ResultPage<RegisteredProtectedResourceResponse>> execute(ListRoleResourcesRequest request) {
        return gate.execute(request.administration()).then(roles.findByIdForTenant(request.roleId(), request.administration().tenantId())
                .switchIfEmpty(Mono.error(new RoleNotFoundException(request.roleId())))
                .filter(role -> role.scope().applicationId().filter(request.administration().applicationId()::equals).isPresent())
                .switchIfEmpty(Mono.error(new RoleNotFoundException(request.roleId())))
                .flatMap(role -> {
                    String query = request.query().toLowerCase(Locale.ROOT);
                    return Flux.fromIterable(role.resources().stream().sorted((a, b) -> a.value().toString().compareTo(b.value().toString())).toList())
                            .concatMap(id -> resources.findByIdForTenant(id, request.administration().tenantId()))
                            .filter(resource -> query.isEmpty() || resource.path().value().toLowerCase(Locale.ROOT).contains(query)
                                    || resource.method().name().toLowerCase(Locale.ROOT).contains(query))
                            .map(resource -> new RegisteredProtectedResourceResponse(resource.id(), resource.applicationId(), resource.tenantId(), resource.path(), resource.method(), resource.registeredAt()))
                            .collectList().map(filtered -> ResultPage.of(filtered.stream().skip(request.window().offset())
                                    .limit(request.window().limit()).toList(), filtered.size(), request.window()));
                }));
    }
}
