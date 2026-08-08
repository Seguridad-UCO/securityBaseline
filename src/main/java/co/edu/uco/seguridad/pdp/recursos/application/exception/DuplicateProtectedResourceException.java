package co.edu.uco.seguridad.pdp.recursos.application.exception;

import co.edu.uco.seguridad.crosscutting.messages.RecursosMessages;
import co.edu.uco.seguridad.pdp.commons.exception.ConflictBusinessRuleException;
import co.edu.uco.seguridad.pdp.recursos.domain.ActionCode;
import co.edu.uco.seguridad.pdp.recursos.domain.ResourceCode;

/**
 * Generado por {@code ProtectedResourceMustBeUniqueRule} y por nada más.
 *
 * <p>Distinto de {@code DuplicateApplicationException}: aquí la aplicación es legítima y
 * solo se repite la concesión, por lo que la solución del llamador es diferente.</p>
 *
 * <p>Vive en {@code application} porque la regla que la lanza necesita el repositorio para detectar
 * el duplicado — no es un invariante que {@code ResourceCode}/{@code ActionCode} puedan verificar solos.</p>
 */
public final class DuplicateProtectedResourceException extends ConflictBusinessRuleException {

    public DuplicateProtectedResourceException(ResourceCode resourceCode, ActionCode action) {
        super("PROTECTED_RESOURCE_ALREADY_EXISTS",
                RecursosMessages.duplicateProtectedResource(resourceCode.value(), action.value()));
    }
}
