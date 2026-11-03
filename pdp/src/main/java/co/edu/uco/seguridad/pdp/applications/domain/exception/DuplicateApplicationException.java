package co.edu.uco.seguridad.pdp.applications.domain.exception;

import co.edu.uco.seguridad.pdp.applications.domain.message.ApplicationsMessages;
import co.edu.uco.seguridad.pdp.commons.exception.ConflictBusinessRuleException;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationName;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;

/**
 * Generada por {@code ApplicationNameMustBeUniqueForTenantRule} y por nada más.
 *
 * <p>Vive en {@code domain} junto a la regla que la lanza. Que la unicidad necesite consultar el
 * almacén no la convierte en un concepto de aplicación: la consulta la hace el validador, y lo que
 * queda aquí —"si ya estaba registrado, rechaza"— es conocimiento del negocio.</p>
 */
public final class DuplicateApplicationException extends ConflictBusinessRuleException {

    public DuplicateApplicationException(TenantId tenantId, ApplicationName name) {
        super("APPLICATION_ALREADY_EXISTS",
                ApplicationsMessages.duplicateApplication(tenantId.value(), name.value()));
    }
}
