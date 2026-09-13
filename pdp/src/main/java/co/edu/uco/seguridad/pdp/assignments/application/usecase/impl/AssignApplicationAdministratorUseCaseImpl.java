package co.edu.uco.seguridad.pdp.assignments.application.usecase.impl;

import co.edu.uco.seguridad.pdp.assignments.application.primaryport.request.AssignApplicationAdministratorRequest;
import co.edu.uco.seguridad.pdp.assignments.application.primaryport.request.AssignRoleRequest;
import co.edu.uco.seguridad.pdp.assignments.application.primaryport.response.AssignmentResponse;
import co.edu.uco.seguridad.pdp.assignments.application.usecase.AssignApplicationAdministratorUseCase;
import co.edu.uco.seguridad.pdp.assignments.application.usecase.AssignRoleUseCase;
import co.edu.uco.seguridad.pdp.roles.application.primaryport.request.DefineRoleRequest;
import co.edu.uco.seguridad.pdp.roles.application.primaryport.request.RoleNameInScopeQuery;
import co.edu.uco.seguridad.pdp.roles.application.primaryport.response.RoleResponse;
import co.edu.uco.seguridad.pdp.roles.application.rule.validator.RoleLookupByNameInScopeValidator;
import co.edu.uco.seguridad.pdp.roles.application.usecase.DefineRoleUseCase;
import co.edu.uco.seguridad.pdp.roles.domain.model.RoleName;
import co.edu.uco.seguridad.pdp.roles.domain.model.RoleScope;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * Implementación de {@link AssignApplicationAdministratorUseCase} (HU-015). Pendiente:
 * {@code roleLookup.execute(new RoleNameInScopeQuery(new RoleName("ADMIN"), RoleScope.ofApplication(tenantId, applicationId)))}
 * — si está vacío, {@code defineRole.execute(...)} lo crea; si no, se reutiliza el existente (caso
 * documentado en la política: "cualquier tenant puede hoy crear un rol llamado ADMIN sin intención
 * administrativa" — este backfill debe adoptarlo, no duplicarlo). Con el {@code RoleId} resuelto,
 * {@code assignRole.execute} lo asigna al {@code userId} recibido. Consume el rol ajeno como
 * validador publicado, nunca consultando {@code RoleRepository} directamente (sb-arquitectura,
 * regla invariante 11 — descubierto por {@code ModulithStructureTests} en la FASE 3 de pruebas).
 */
public final class AssignApplicationAdministratorUseCaseImpl implements AssignApplicationAdministratorUseCase {

    private static final RoleName ADMIN_ROLE_NAME = new RoleName("ADMIN");

    private final RoleLookupByNameInScopeValidator roleLookup;
    private final DefineRoleUseCase defineRole;
    private final AssignRoleUseCase assignRole;

    public AssignApplicationAdministratorUseCaseImpl(RoleLookupByNameInScopeValidator roleLookup,
            DefineRoleUseCase defineRole, AssignRoleUseCase assignRole) {
        this.roleLookup = Objects.requireNonNull(roleLookup, RequiredArgumentMessages.ROLE_LOOKUP_BY_NAME_IN_SCOPE_VALIDATOR);
        this.defineRole = Objects.requireNonNull(defineRole, RequiredArgumentMessages.DEFINE_ROLE_USE_CASE);
        this.assignRole = Objects.requireNonNull(assignRole, RequiredArgumentMessages.ASSIGN_ROLE_USE_CASE);
    }

    @Override
    public Mono<AssignmentResponse> execute(AssignApplicationAdministratorRequest input) {
        RoleScope scope = RoleScope.ofApplication(input.tenantId(), input.applicationId());
        return roleLookup.execute(new RoleNameInScopeQuery(ADMIN_ROLE_NAME, scope))
                .switchIfEmpty(Mono.defer(() -> defineRole.execute(new DefineRoleRequest(ADMIN_ROLE_NAME, scope))
                        .map(RoleResponse::id)))
                .flatMap(roleId -> assignRole.execute(
                        new AssignRoleRequest(input.tenantId(), input.userId(), input.applicationId(), roleId)));
    }
}
