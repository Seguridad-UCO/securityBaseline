package co.edu.uco.seguridad.pdp.roles.domain.exception;

import co.edu.uco.seguridad.pdp.commons.exception.BusinessRuleViolationException;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.ResourceId;
import co.edu.uco.seguridad.pdp.roles.domain.message.RolesMessages;

/**
 * El alcance del rol no cubre la aplicación (o el inquilino) del recurso: INV-DAT-01.
 */
public final class ResourceOutsideRoleScopeException extends BusinessRuleViolationException {

    public ResourceOutsideRoleScopeException(ResourceId resourceId) {
        super("RESOURCE_OUTSIDE_ROLE_SCOPE", RolesMessages.resourceOutsideRoleScope(resourceId.value().toString()));
    }

    /**
     * {@code RoleScopeMustCoverResourceRule} es pura y solo recibe {@code ResourceCoverage}
     * (scope, aplicación e inquilino del recurso) — nunca el {@code ResourceId}, que es del
     * validador que la invoca. Este constructor es el que la regla usa de verdad.
     */
    public ResourceOutsideRoleScopeException(ApplicationId resourceApplicationId) {
        super("RESOURCE_OUTSIDE_ROLE_SCOPE",
                RolesMessages.resourceOutsideRoleScopeForApplication(resourceApplicationId.value().toString()));
    }
}
