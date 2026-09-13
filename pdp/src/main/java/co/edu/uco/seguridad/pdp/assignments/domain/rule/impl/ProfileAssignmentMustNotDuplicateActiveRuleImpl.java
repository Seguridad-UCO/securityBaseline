package co.edu.uco.seguridad.pdp.assignments.domain.rule.impl;

import co.edu.uco.seguridad.pdp.assignments.domain.exception.DuplicateProfileAssignmentException;
import co.edu.uco.seguridad.pdp.assignments.domain.rule.ProfileAssignmentMustNotDuplicateActiveRule;
import co.edu.uco.seguridad.pdp.assignments.domain.rule.model.ActiveProfileAssignmentAvailability;

public final class ProfileAssignmentMustNotDuplicateActiveRuleImpl implements ProfileAssignmentMustNotDuplicateActiveRule {

    @Override
    public void execute(ActiveProfileAssignmentAvailability input) {
        if (input.taken()) {
            throw new DuplicateProfileAssignmentException(input.userId(), input.applicationId(), input.profileId());
        }
    }
}
