package co.edu.uco.seguridad.pdp.authorization.domain.rule;

import co.edu.uco.seguridad.pdp.authorization.domain.rule.model.UserTenantAssignment;
import co.edu.uco.seguridad.shared.contract.OperationWithoutResult;

/**
 * Reasignar el inquilino de un usuario exige administrar el catálogo de los dos inquilinos que toca:
 * el que el usuario deja y el que recibe (SU-01, ADR-036). Un actor administra solo el suyo, así que
 * mover a alguien entre inquilinos queda cerrado hasta que exista el administrador global (ADR-024).
 */
public interface UserTenantAssignmentMustStayInActorTenantRule extends OperationWithoutResult<UserTenantAssignment> {
}
