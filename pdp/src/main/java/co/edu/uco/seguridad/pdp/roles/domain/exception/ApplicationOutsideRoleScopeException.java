package co.edu.uco.seguridad.pdp.roles.domain.exception;

import co.edu.uco.seguridad.pdp.commons.exception.BusinessRuleViolationException;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.roles.domain.message.RolesMessages;

/** El alcance del rol no cubre la aplicación indicada (INV-ASN-02, HU-005). */
public final class ApplicationOutsideRoleScopeException extends BusinessRuleViolationException {

    public ApplicationOutsideRoleScopeException(ApplicationId applicationId) {
        super("APPLICATION_OUTSIDE_ROLE_SCOPE", RolesMessages.applicationOutsideRoleScope(applicationId.value().toString()));
    }
}
