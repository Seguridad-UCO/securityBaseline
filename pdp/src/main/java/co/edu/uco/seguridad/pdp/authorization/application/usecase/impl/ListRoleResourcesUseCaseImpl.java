package co.edu.uco.seguridad.pdp.authorization.application.usecase.impl;

import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.ListRoleResourcesRequest;
import co.edu.uco.seguridad.pdp.authorization.application.rule.validator.PrincipalMustBeApplicationAdministratorValidator;
import co.edu.uco.seguridad.pdp.authorization.application.usecase.ListRoleResourcesUseCase;
import co.edu.uco.seguridad.pdp.commons.model.ResourceId;
import co.edu.uco.seguridad.pdp.commons.model.ResultPage;
import co.edu.uco.seguridad.pdp.resources.application.primaryport.response.RegisteredProtectedResourceResponse;
import co.edu.uco.seguridad.pdp.resources.application.secondaryport.repository.ProtectedResourceRepository;
import co.edu.uco.seguridad.pdp.roles.application.secondaryport.repository.RoleRepository;
import co.edu.uco.seguridad.pdp.roles.domain.exception.RoleNotFoundException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Does pagination before materialising resources; the browser never receives a role-wide catalog. */
public final class ListRoleResourcesUseCaseImpl implements ListRoleResourcesUseCase {
    private final PrincipalMustBeApplicationAdministratorValidator gate; private final RoleRepository roles; private final ProtectedResourceRepository resources;
    public ListRoleResourcesUseCaseImpl(PrincipalMustBeApplicationAdministratorValidator gate, RoleRepository roles, ProtectedResourceRepository resources) { this.gate = gate; this.roles = roles; this.resources = resources; }
    @Override public Mono<ResultPage<RegisteredProtectedResourceResponse>> execute(ListRoleResourcesRequest request) {
        return gate.execute(request.administration()).then(roles.findByIdForTenant(request.roleId(), request.administration().tenantId())
                .switchIfEmpty(Mono.error(new RoleNotFoundException(request.roleId())))
                .filter(role -> role.scope().applicationId().filter(request.administration().applicationId()::equals).isPresent())
                .switchIfEmpty(Mono.error(new RoleNotFoundException(request.roleId())))
                .flatMap(role -> {
                    var ids = role.resources().stream().sorted((a, b) -> a.value().toString().compareTo(b.value().toString())).toList();
                    var slice = ids.stream().skip(request.window().offset()).limit(request.window().limit()).toList();
                    return Flux.fromIterable(slice).flatMap(id -> resources.findByIdForTenant(id, request.administration().tenantId()))
                            .map(resource -> new RegisteredProtectedResourceResponse(resource.id(), resource.applicationId(), resource.tenantId(), resource.path(), resource.method(), resource.registeredAt()))
                            .collectList().map(content -> ResultPage.of(content, ids.size(), request.window()));
                }));
    }
}
