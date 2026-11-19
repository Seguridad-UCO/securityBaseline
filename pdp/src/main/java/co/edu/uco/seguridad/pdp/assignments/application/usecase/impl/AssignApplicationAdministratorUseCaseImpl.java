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
 * Implementación de {@link AssignApplicationAdministratorUseCase} (HU-015). Encuentra o crea el
 * rol {@code ADMIN} de la aplicación y lo asigna. No audita (HU-021): su request no trae
 * {@code subject}, y ya se invoca exclusivamente detrás de
 * {@code AdministerApplicationAdministratorAssignmentUseCaseImpl} (HU-020), que sí tiene el
 * {@code AdministrationRequest} completo y ya audita {@code ADMINISTRATOR_ASSIGNED} — auditar
 * aquí también duplicaría el evento (ver PLAN-HU-021.md, nota de retrofit).
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
