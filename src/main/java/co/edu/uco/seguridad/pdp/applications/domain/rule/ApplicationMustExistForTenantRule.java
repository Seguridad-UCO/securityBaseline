package co.edu.uco.seguridad.pdp.applications.domain.rule;

import co.edu.uco.seguridad.pdp.applications.domain.rule.model.ApplicationExistence;
import co.edu.uco.seguridad.shared.contract.OperationWithoutResult;

/** La aplicación referenciada tiene que existir dentro del inquilino que la referencia. */
public interface ApplicationMustExistForTenantRule extends OperationWithoutResult<ApplicationExistence> {
}
