package co.edu.uco.seguridad.pdp.roles.application.usecase.impl;

import co.edu.uco.seguridad.pdp.roles.application.primaryport.request.FindRoleForTenantRequest;
import co.edu.uco.seguridad.pdp.roles.application.primaryport.response.RoleResponse;
import co.edu.uco.seguridad.pdp.roles.application.secondaryport.repository.RoleRepository;
import co.edu.uco.seguridad.pdp.roles.application.usecase.FindRoleForTenantUseCase;
import reactor.core.publisher.Mono;

import java.util.Objects;

public final class FindRoleForTenantUseCaseImpl implements FindRoleForTenantUseCase {
    private final RoleRepository roles;

    public FindRoleForTenantUseCaseImpl(RoleRepository roles) {
        this.roles = Objects.requireNonNull(roles);
    }

    @Override
    public Mono<RoleResponse> execute(FindRoleForTenantRequest input) {
        return roles.findByIdForTenant(input.roleId(), input.tenantId()).map(role -> new RoleResponse(role.id(),
                role.name(), role.scope(), role.resources(), role.registeredAt()));
    }
}
