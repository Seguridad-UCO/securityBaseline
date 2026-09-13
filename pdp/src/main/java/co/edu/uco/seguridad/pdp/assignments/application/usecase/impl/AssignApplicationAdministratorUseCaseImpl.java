package co.edu.uco.seguridad.pdp.assignments.application.usecase.impl;

import co.edu.uco.seguridad.pdp.assignments.application.primaryport.request.AssignApplicationAdministratorRequest;
import co.edu.uco.seguridad.pdp.assignments.application.primaryport.response.AssignmentResponse;
import co.edu.uco.seguridad.pdp.assignments.application.usecase.AssignApplicationAdministratorUseCase;
import co.edu.uco.seguridad.pdp.assignments.application.usecase.AssignRoleUseCase;
import co.edu.uco.seguridad.pdp.roles.application.secondaryport.repository.RoleRepository;
import co.edu.uco.seguridad.pdp.roles.application.usecase.DefineRoleUseCase;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * Implementación de {@link AssignApplicationAdministratorUseCase} (HU-015). Pendiente:
 * {@code roleRepository.findByNameInScope(new RoleName("ADMIN"), RoleScope.ofApplication(tenantId, applicationId))}
 * — si está vacío, {@code defineRole.execute(...)} lo crea; si no, se reutiliza el existente (caso
 * documentado en la política: "cualquier tenant puede hoy crear un rol llamado ADMIN sin intención
 * administrativa" — este backfill debe adoptarlo, no duplicarlo). Con el {@code RoleId} resuelto,
 * {@code assignRole.execute} lo asigna al {@code userId} recibido.
 */
public final class AssignApplicationAdministratorUseCaseImpl implements AssignApplicationAdministratorUseCase {

    private final RoleRepository roleRepository;
    private final DefineRoleUseCase defineRole;
    private final AssignRoleUseCase assignRole;

    public AssignApplicationAdministratorUseCaseImpl(RoleRepository roleRepository, DefineRoleUseCase defineRole,
            AssignRoleUseCase assignRole) {
        this.roleRepository = Objects.requireNonNull(roleRepository, RequiredArgumentMessages.ROLE_REPOSITORY);
        this.defineRole = Objects.requireNonNull(defineRole, RequiredArgumentMessages.DEFINE_ROLE_USE_CASE);
        this.assignRole = Objects.requireNonNull(assignRole, RequiredArgumentMessages.ASSIGN_ROLE_USE_CASE);
    }

    @Override
    public Mono<AssignmentResponse> execute(AssignApplicationAdministratorRequest input) {
        throw new UnsupportedOperationException("pendiente: HU-015");
    }
}
