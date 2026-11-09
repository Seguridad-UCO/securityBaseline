package co.edu.uco.seguridad.pdp.assignments.domain.rule.impl;

import co.edu.uco.seguridad.pdp.assignments.domain.exception.ProfileAssignmentNotFoundException;
import co.edu.uco.seguridad.pdp.assignments.domain.rule.ProfileAssignmentMustExistForTenantRule;
import co.edu.uco.seguridad.pdp.assignments.domain.rule.model.ProfileAssignmentExistence;

public final class ProfileAssignmentMustExistForTenantRuleImpl implements ProfileAssignmentMustExistForTenantRule {

    @Override
    public void execute(ProfileAssignmentExistence input) {
        if (!input.registered()) {
            throw new ProfileAssignmentNotFoundException(input.profileAssignmentId());
        }
    }
}
