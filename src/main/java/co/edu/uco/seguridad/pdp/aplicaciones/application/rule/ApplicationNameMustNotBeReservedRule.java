package co.edu.uco.seguridad.pdp.aplicaciones.application.rule;

import co.edu.uco.seguridad.pdp.commons.ApplicationName;
import co.edu.uco.seguridad.shared.contract.OperationWithoutResult;

/**
 * Regla sin repositorio: los nombres reservados por la plataforma nunca pueden ser registrados. Es
 * una regla, no un invariante de {@link ApplicationName}, porque la lista reservada es política, no
 * parte del concepto.
 */
public interface ApplicationNameMustNotBeReservedRule extends OperationWithoutResult<ApplicationName> {
}
