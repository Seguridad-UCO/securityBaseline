package co.edu.uco.seguridad.pdp.roles.application.primaryport.request;

import co.edu.uco.seguridad.pdp.roles.domain.model.RoleName;
import co.edu.uco.seguridad.pdp.roles.domain.model.RoleScope;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;

import java.util.Objects;

/**
 * Entrada de {@code RoleLookupByNameInScopeValidator} (HU-015): qué nombre de rol, en qué alcance
 * exacto. Los contratos base admiten un solo parámetro, así que la pareja viaja como un tipo propio
 * (mismo patrón que {@code ApplicationOwnershipQuery}).
 */
public record RoleNameInScopeQuery(RoleName name, RoleScope scope) {

    public RoleNameInScopeQuery {
        Objects.requireNonNull(name, RequiredArgumentMessages.ROLE_NAME);
        Objects.requireNonNull(scope, RequiredArgumentMessages.ROLE_SCOPE);
    }
}
