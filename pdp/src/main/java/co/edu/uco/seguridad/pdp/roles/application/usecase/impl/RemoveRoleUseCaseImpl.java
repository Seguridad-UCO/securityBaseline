package co.edu.uco.seguridad.pdp.roles.application.usecase.impl;
import co.edu.uco.seguridad.pdp.roles.application.secondaryport.repository.RoleRepository;
import co.edu.uco.seguridad.pdp.roles.application.usecase.RemoveRoleUseCase;
import co.edu.uco.seguridad.pdp.roles.domain.exception.ProtectedRoleException;
import co.edu.uco.seguridad.pdp.roles.domain.exception.RoleNotFoundException;
import reactor.core.publisher.Mono;
public final class RemoveRoleUseCaseImpl implements RemoveRoleUseCase {
    private static final String ADMIN = "ADMIN";
    private final RoleRepository repository;
    public RemoveRoleUseCaseImpl(RoleRepository repository) { this.repository = repository; }
    @Override public Mono<Void> execute(Request input) { return repository.findByIdForTenant(input.roleId(), input.tenantId())
            .switchIfEmpty(Mono.error(() -> new RoleNotFoundException(input.roleId())))
            .flatMap(role -> isDefaultApplicationAdministrator(role)
                    ? Mono.error(new ProtectedRoleException(role.id()))
                    : repository.deleteById(role.id())); }
    private static boolean isDefaultApplicationAdministrator(co.edu.uco.seguridad.pdp.roles.domain.Role role) {
        return ADMIN.equalsIgnoreCase(role.name().value()) && role.scope().applicationId().isPresent();
    }
}
