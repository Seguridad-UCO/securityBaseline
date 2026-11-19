package co.edu.uco.seguridad.pdp.authorization.domain.exception;

import co.edu.uco.seguridad.pdp.authorization.domain.message.AuthorizationMessages;
import co.edu.uco.seguridad.pdp.commons.exception.BusinessRuleViolationException;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;

/**
 * El sujeto está autorizado por rol para administrar la aplicación indicada, pero su sesión no trae
 * evidencia de MFA aceptada (HU-024, ADR-027). Distinta de {@link NotAuthorizedToAdministerException}
 * a propósito: el sujeto necesita reautenticar con su segundo factor, no pedir otro rol.
 * {@code tenantId} viaja igual que en esa excepción, por si el manejador de errores necesita
 * discriminar por inquilino en la traza.
 */
public final class MfaEvidenceRequiredException extends BusinessRuleViolationException {

    public MfaEvidenceRequiredException(TenantId tenantId, ApplicationId applicationId) {
        super("MFA_REQUIRED", AuthorizationMessages.mfaEvidenceRequired(applicationId));
    }
}
