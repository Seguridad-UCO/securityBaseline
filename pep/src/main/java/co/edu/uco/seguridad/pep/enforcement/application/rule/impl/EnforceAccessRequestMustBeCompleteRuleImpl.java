package co.edu.uco.seguridad.pep.enforcement.application.rule.impl;

import co.edu.uco.seguridad.pep.commons.EnforcementFailure;
import co.edu.uco.seguridad.pep.enforcement.application.port.primary.dto.request.EnforceAccessRequest;
import co.edu.uco.seguridad.pep.enforcement.application.rule.EnforceAccessRequestMustBeCompleteRule;

public final class EnforceAccessRequestMustBeCompleteRuleImpl implements EnforceAccessRequestMustBeCompleteRule {
    @Override
    public void execute(EnforceAccessRequest input) {
        if (input == null || input.accessRequest() == null || input.identityEvidence() == null
                || input.identityEvidence().bearer().isBlank()) {
            throw new EnforcementFailure(EnforcementFailure.Kind.INVALID_REQUEST, "INVALID_ACCESS_REQUEST");
        }
    }
}
