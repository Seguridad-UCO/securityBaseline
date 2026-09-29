package co.edu.uco.seguridad.pdp.roles.application.usecase.impl;

import co.edu.uco.seguridad.pdp.commons.model.ResultPage;
import co.edu.uco.seguridad.pdp.roles.application.primaryport.request.ListApplicationRolesPageRequest;
import co.edu.uco.seguridad.pdp.roles.application.primaryport.response.RoleResponse;
import co.edu.uco.seguridad.pdp.roles.application.secondaryport.repository.RoleRepository;
import co.edu.uco.seguridad.pdp.roles.application.usecase.ListApplicationRolesPageUseCase;
import co.edu.uco.seguridad.pdp.roles.domain.RoleCriteria;
import reactor.core.publisher.Mono;

public final class ListApplicationRolesPageUseCaseImpl implements ListApplicationRolesPageUseCase {
    private final RoleRepository repository;
    public ListApplicationRolesPageUseCaseImpl(RoleRepository repository) { this.repository = repository; }
    @Override public Mono<ResultPage<RoleResponse>> execute(ListApplicationRolesPageRequest request) {
        return repository.findBy(RoleCriteria.ofApplication(request.tenantId(), request.applicationId()), request.window())
                .map(page -> page.map(role -> new RoleResponse(role.id(), role.name(), role.scope(), role.resources(), role.registeredAt())));
    }
}
