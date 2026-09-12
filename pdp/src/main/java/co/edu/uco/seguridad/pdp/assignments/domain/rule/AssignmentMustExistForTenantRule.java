package co.edu.uco.seguridad.pdp.assignments.domain.rule;

import co.edu.uco.seguridad.pdp.assignments.domain.rule.model.AssignmentExistence;
import co.edu.uco.seguridad.shared.contract.OperationWithoutResult;

/** La asignación referenciada tiene que existir para ese tenant. */
public interface AssignmentMustExistForTenantRule extends OperationWithoutResult<AssignmentExistence> {
}
