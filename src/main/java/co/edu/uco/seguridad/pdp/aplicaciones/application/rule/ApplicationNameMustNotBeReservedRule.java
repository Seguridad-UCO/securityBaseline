package co.edu.uco.seguridad.pdp.aplicaciones.application.rule;

import co.edu.uco.seguridad.pdp.commons.ApplicationName;
import co.edu.uco.seguridad.shared.contract.OperationWithoutResult;

/**
 * Regla sin repositorio: los nombres reservados por la plataforma nunca pueden ser registrados por un inquilino.
 *
 * <p>Es una regla y no un invariante de {@link ApplicationName} porque la lista reservada es
 * política que cambia con la plataforma, mientras que el formato del objeto de valor es parte del concepto
 * en sí.</p>
 */
public interface ApplicationNameMustNotBeReservedRule extends OperationWithoutResult<ApplicationName> {
}
