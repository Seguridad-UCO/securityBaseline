package co.edu.uco.seguridad.pdp.roles.application.usecase.impl;

import co.edu.uco.seguridad.pdp.roles.application.secondaryport.repository.RoleRepository;
import co.edu.uco.seguridad.pdp.roles.application.primaryport.request.ApplicationRoleCountRequest;
import co.edu.uco.seguridad.pdp.roles.application.usecase.CountApplicationRolesUseCase;
import reactor.core.publisher.Mono;

public final class CountApplicationRolesUseCaseImpl implements CountApplicationRolesUseCase {
    private final RoleRepository repository;

    public CountApplicationRolesUseCaseImpl(RoleRepository repository) {
        this.repository = repository;
    }

    @Override
    public Mono<Long> execute(ApplicationRoleCountRequest request) {
        return repository.countByApplication(request.tenantId(), request.applicationId());
    }
}
