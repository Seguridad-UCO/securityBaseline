package co.edu.uco.seguridad.pdp.assignments.application.usecase.impl;

import co.edu.uco.seguridad.pdp.applications.application.primaryport.response.ApplicationRegistrationResponse;
import co.edu.uco.seguridad.pdp.applications.application.usecase.RegisterApplicationUseCase;
import co.edu.uco.seguridad.pdp.assignments.application.primaryport.request.AssignRoleRequest;
import co.edu.uco.seguridad.pdp.assignments.application.primaryport.request.RegisterApplicationWithFirstAdministratorRequest;
import co.edu.uco.seguridad.pdp.assignments.application.usecase.AssignRoleUseCase;
import co.edu.uco.seguridad.pdp.assignments.application.usecase.RegisterApplicationWithFirstAdministratorUseCase;
import co.edu.uco.seguridad.pdp.roles.application.primaryport.request.DefineRoleRequest;
import co.edu.uco.seguridad.pdp.roles.application.usecase.DefineRoleUseCase;
import co.edu.uco.seguridad.pdp.roles.domain.model.RoleName;
import co.edu.uco.seguridad.pdp.roles.domain.model.RoleScope;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * Implementación de {@link RegisterApplicationWithFirstAdministratorUseCase} (HU-015). Pendiente:
 * {@code registerApplication.execute(dto)} → con la aplicación registrada, {@code defineRole.execute}
 * con {@code RoleName("ADMIN")} y {@code RoleScope.ofApplication(dto.application().tenantId(), registered.id())}
 * → {@code assignRole.execute} al {@code dto.registrarUserId()} → devuelve la respuesta de registro
 * original sin modificar. Si el registro falla, no se define ningún rol ni asignación (falla antes
 * del primer paso).
 */
public final class RegisterApplicationWithFirstAdministratorUseCaseImpl
        implements RegisterApplicationWithFirstAdministratorUseCase {

    private static final RoleName ADMIN_ROLE_NAME = new RoleName("ADMIN");

    private final RegisterApplicationUseCase registerApplication;
    private final DefineRoleUseCase defineRole;
    private final AssignRoleUseCase assignRole;

    public RegisterApplicationWithFirstAdministratorUseCaseImpl(RegisterApplicationUseCase registerApplication,
            DefineRoleUseCase defineRole, AssignRoleUseCase assignRole) {
        this.registerApplication = Objects.requireNonNull(registerApplication, RequiredArgumentMessages.REGISTER_APPLICATION_USE_CASE);
        this.defineRole = Objects.requireNonNull(defineRole, RequiredArgumentMessages.DEFINE_ROLE_USE_CASE);
        this.assignRole = Objects.requireNonNull(assignRole, RequiredArgumentMessages.ASSIGN_ROLE_USE_CASE);
    }

    @Override
    public Mono<ApplicationRegistrationResponse> execute(RegisterApplicationWithFirstAdministratorRequest dto) {
        return registerApplication.execute(dto.application())
                .flatMap(registered -> {
                    RoleScope scope = RoleScope.ofApplication(dto.application().tenantId(),
                            registered.application().id());
                    return defineRole.execute(new DefineRoleRequest(ADMIN_ROLE_NAME, scope))
                            .flatMap(role -> assignRole.execute(new AssignRoleRequest(dto.application().tenantId(),
                                    dto.registrarUserId(), registered.application().id(), role.id())))
                            .thenReturn(registered);
                });
    }
}
