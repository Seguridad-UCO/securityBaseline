package co.edu.uco.seguridad.pdp.roles.domain.rule;

import co.edu.uco.seguridad.pdp.roles.domain.rule.model.RoleNameAvailability;
import co.edu.uco.seguridad.shared.contract.OperationWithoutResult;

/**
 * Regla R1: el nombre es único dentro del alcance exacto. Pura y síncrona: recibe el hecho resuelto y decide.
 */
public interface RoleNameMustBeUniqueInScopeRule extends OperationWithoutResult<RoleNameAvailability> {
}
