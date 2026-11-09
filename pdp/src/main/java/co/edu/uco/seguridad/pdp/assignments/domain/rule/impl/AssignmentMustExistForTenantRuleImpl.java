package co.edu.uco.seguridad.pdp.assignments.domain.rule.impl;

import co.edu.uco.seguridad.pdp.assignments.domain.exception.AssignmentNotFoundException;
import co.edu.uco.seguridad.pdp.assignments.domain.rule.AssignmentMustExistForTenantRule;
import co.edu.uco.seguridad.pdp.assignments.domain.rule.model.AssignmentExistence;

public final class AssignmentMustExistForTenantRuleImpl implements AssignmentMustExistForTenantRule {

    @Override
    public void execute(AssignmentExistence input) {
        if (!input.registered()) {
            throw new AssignmentNotFoundException(input.assignmentId());
        }
    }
}
