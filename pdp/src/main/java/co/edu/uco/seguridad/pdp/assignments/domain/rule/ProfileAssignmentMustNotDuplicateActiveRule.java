package co.edu.uco.seguridad.pdp.assignments.domain.rule;

import co.edu.uco.seguridad.pdp.assignments.domain.rule.model.ActiveProfileAssignmentAvailability;
import co.edu.uco.seguridad.shared.contract.OperationWithoutResult;

public interface ProfileAssignmentMustNotDuplicateActiveRule extends OperationWithoutResult<ActiveProfileAssignmentAvailability> {
}
