package co.edu.uco.seguridad.pdp.roles.domain.rule;

import co.edu.uco.seguridad.pdp.roles.domain.rule.model.RoleExistence;
import co.edu.uco.seguridad.shared.contract.OperationWithoutResult;

/**
 * Regla R3: el rol existe para ese inquilino. Un rol ajeno o global no existe para quien pregunta.
 */
public interface RoleMustExistForTenantRule extends OperationWithoutResult<RoleExistence> {
}
