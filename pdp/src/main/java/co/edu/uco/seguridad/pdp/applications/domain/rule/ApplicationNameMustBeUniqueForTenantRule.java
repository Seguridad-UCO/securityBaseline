package co.edu.uco.seguridad.pdp.applications.domain.rule;

import co.edu.uco.seguridad.pdp.applications.domain.rule.model.ApplicationNameAvailability;
import co.edu.uco.seguridad.shared.contract.OperationWithoutResult;

/** Un nombre de aplicación es único dentro de un inquilino, no globalmente. */
public interface ApplicationNameMustBeUniqueForTenantRule extends OperationWithoutResult<ApplicationNameAvailability> {
}
