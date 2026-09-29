package co.edu.uco.seguridad.pdp.roles.application.usecase.impl;

import co.edu.uco.seguridad.pdp.roles.application.primaryport.request.UpdateRoleRequest;
import co.edu.uco.seguridad.pdp.roles.application.primaryport.response.RoleResponse;
import co.edu.uco.seguridad.pdp.roles.application.secondaryport.repository.RoleRepository;
import co.edu.uco.seguridad.pdp.roles.application.usecase.UpdateRoleUseCase;
import co.edu.uco.seguridad.pdp.roles.domain.exception.DuplicateRoleNameException;
import co.edu.uco.seguridad.pdp.roles.domain.exception.ProtectedRoleException;
import co.edu.uco.seguridad.pdp.roles.domain.exception.RoleNotFoundException;
import reactor.core.publisher.Mono;
public final class UpdateRoleUseCaseImpl implements UpdateRoleUseCase {
    private static final String ADMIN = "ADMIN";
    private final RoleRepository repository;
    public UpdateRoleUseCaseImpl(RoleRepository repository) { this.repository = repository; }
    @Override public Mono<RoleResponse> execute(UpdateRoleRequest input) {
        return repository.findByIdForTenant(input.roleId(), input.tenantId())
                .switchIfEmpty(Mono.error(() -> new RoleNotFoundException(input.roleId())))
                .flatMap(current -> isDefaultApplicationAdministrator(current)
                        ? Mono.error(new ProtectedRoleException(current.id()))
                        : repository.existsByNameInScope(input.name(), current.scope())
                        .filter(taken -> taken && !current.name().equals(input.name()))
                        .flatMap(taken -> Mono.<co.edu.uco.seguridad.pdp.roles.domain.Role>error(
                                new DuplicateRoleNameException(input.name(), current.scope())))
                        .switchIfEmpty(repository.save(current.withName(input.name()))))
                .map(role -> new RoleResponse(role.id(), role.name(), role.scope(), role.resources(), role.registeredAt()));
    }
    private static boolean isDefaultApplicationAdministrator(co.edu.uco.seguridad.pdp.roles.domain.Role role) {
        return ADMIN.equalsIgnoreCase(role.name().value()) && role.scope().applicationId().isPresent();
    }
}
