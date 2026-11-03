package co.edu.uco.seguridad.pdp.authorization.domain.exception;

import co.edu.uco.seguridad.pdp.authorization.domain.message.AuthorizationMessages;
import co.edu.uco.seguridad.pdp.commons.exception.BusinessRuleViolationException;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;

/**
 * El sujeto no tiene, para el inquilino y la aplicación indicados, una decisión {@code ALLOW} de
 * {@code AdministrationDecisionPort} (HU-009). {@code tenantId} no se usa hoy en el mensaje, pero
 * viaja para cuando el manejador de errores necesite discriminar por inquilino en la traza.
 */
public final class NotAuthorizedToAdministerException extends BusinessRuleViolationException {

    public NotAuthorizedToAdministerException(TenantId tenantId, ApplicationId applicationId) {
        super("NOT_AUTHORIZED_TO_ADMINISTER", AuthorizationMessages.notAuthorizedToAdminister(applicationId));
    }
}
