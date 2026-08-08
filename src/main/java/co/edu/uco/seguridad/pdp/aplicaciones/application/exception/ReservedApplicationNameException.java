package co.edu.uco.seguridad.pdp.aplicaciones.application.exception;

import co.edu.uco.seguridad.crosscutting.messages.ApplicationsMessages;
import co.edu.uco.seguridad.pdp.commons.ApplicationName;
import co.edu.uco.seguridad.pdp.commons.exception.BusinessRuleViolationException;

/**
 * Generado por {@code ApplicationNameMustNotBeReservedRule} y por nada más.
 *
 * <p>Separado de {@link DuplicateApplicationException}: un nombre reservado se rechaza para todo
 * inquilino y para siempre, un duplicado solo para un inquilino y solo mientras el otro registro
 * existe. Colapsar ambos en una excepción ocultaría esa diferencia del cliente.</p>
 *
 * <p>Vive en {@code application} porque la lista de nombres reservados es configuración externa
 * ({@code ApplicationCatalogProperties}), no un invariante propio del objeto de valor.</p>
 */
public final class ReservedApplicationNameException extends BusinessRuleViolationException {

    public ReservedApplicationNameException(ApplicationName name) {
        super("RESERVED_APPLICATION_NAME", ApplicationsMessages.reservedApplicationName(name.value()));
    }
}
