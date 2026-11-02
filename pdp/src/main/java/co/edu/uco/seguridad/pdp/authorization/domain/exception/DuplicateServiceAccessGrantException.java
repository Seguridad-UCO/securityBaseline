package co.edu.uco.seguridad.pdp.authorization.domain.exception;

import co.edu.uco.seguridad.pdp.commons.exception.ConflictBusinessRuleException;

/** Ya existe una solicitud pendiente o un permiso activo para la misma identidad y alcance. */
public final class DuplicateServiceAccessGrantException extends ConflictBusinessRuleException {

    public DuplicateServiceAccessGrantException(String scope) {
        super("SERVICE_ACCESS_GRANT_ALREADY_EXISTS",
                "Ya existe una solicitud o permiso vigente para " + scope + ".");
    }
}
