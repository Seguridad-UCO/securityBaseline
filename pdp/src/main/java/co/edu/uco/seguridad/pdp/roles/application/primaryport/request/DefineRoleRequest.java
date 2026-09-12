package co.edu.uco.seguridad.pdp.roles.application.primaryport.request;

import co.edu.uco.seguridad.pdp.roles.domain.model.RoleName;
import co.edu.uco.seguridad.pdp.roles.domain.model.RoleScope;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;

import java.util.Objects;

/** Entrada tipada de DefineRoleUseCase. El alcance ya trae el inquilino del principal: aquí no queda nada que rechazar. */
public record DefineRoleRequest(RoleName name, RoleScope scope) {

    public DefineRoleRequest {
        Objects.requireNonNull(name, RequiredArgumentMessages.ROLE_NAME);
        Objects.requireNonNull(scope, RequiredArgumentMessages.ROLE_SCOPE);
    }
}
