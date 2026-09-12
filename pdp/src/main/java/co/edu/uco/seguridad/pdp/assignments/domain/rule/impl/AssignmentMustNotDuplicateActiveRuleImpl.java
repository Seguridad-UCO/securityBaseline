package co.edu.uco.seguridad.pdp.assignments.domain.rule.impl;

import co.edu.uco.seguridad.pdp.assignments.domain.exception.DuplicateAssignmentException;
import co.edu.uco.seguridad.pdp.assignments.domain.rule.AssignmentMustNotDuplicateActiveRule;
import co.edu.uco.seguridad.pdp.assignments.domain.rule.model.ActiveAssignmentAvailability;

public final class AssignmentMustNotDuplicateActiveRuleImpl implements AssignmentMustNotDuplicateActiveRule {

    @Override
    public void execute(ActiveAssignmentAvailability input) {
        if (input.taken()) {
            throw new DuplicateAssignmentException(input.userId(), input.applicationId(), input.roleId());
        }
    }
}
