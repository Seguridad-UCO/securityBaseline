package co.edu.uco.seguridad.pdp.assignments.domain.rule.impl;

import co.edu.uco.seguridad.pdp.assignments.domain.exception.CannotRemoveLastAdministratorException;
import co.edu.uco.seguridad.pdp.assignments.domain.rule.LastAdministratorMustNotBeRevokedRule;
import co.edu.uco.seguridad.pdp.assignments.domain.rule.model.AdministratorRevocationEligibility;

public final class LastAdministratorMustNotBeRevokedRuleImpl implements LastAdministratorMustNotBeRevokedRule {

    @Override
    public void execute(AdministratorRevocationEligibility input) {
        if (input.activeAdministratorCount() <= 1) {
            throw new CannotRemoveLastAdministratorException(input.applicationId());
        }
    }
}
