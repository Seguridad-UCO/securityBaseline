package co.edu.uco.seguridad.pdp.assignments.domain.rule;

import co.edu.uco.seguridad.pdp.assignments.domain.rule.model.ActiveAssignmentAvailability;
import co.edu.uco.seguridad.shared.contract.OperationWithoutResult;

/** No debe existir ya una asignación activa para la misma tripleta (usuario, aplicación, rol). */
public interface AssignmentMustNotDuplicateActiveRule extends OperationWithoutResult<ActiveAssignmentAvailability> {
}
