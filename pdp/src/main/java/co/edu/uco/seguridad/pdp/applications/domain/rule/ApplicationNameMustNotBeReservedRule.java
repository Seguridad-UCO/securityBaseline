package co.edu.uco.seguridad.pdp.applications.domain.rule;

import co.edu.uco.seguridad.pdp.commons.model.ApplicationName;
import co.edu.uco.seguridad.shared.contract.OperationWithoutResult;

/**
 * Los nombres reservados por la plataforma nunca pueden registrarse. Es una regla, no un invariante
 * de {@link ApplicationName}, porque la lista reservada es política y cambia con la configuración,
 * no parte del concepto de nombre.
 */
public interface ApplicationNameMustNotBeReservedRule extends OperationWithoutResult<ApplicationName> {
}
