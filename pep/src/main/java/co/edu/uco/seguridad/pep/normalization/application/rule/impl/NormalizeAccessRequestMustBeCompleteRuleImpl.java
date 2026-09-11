package co.edu.uco.seguridad.pep.normalization.application.rule.impl;

import co.edu.uco.seguridad.pep.commons.EnforcementFailure;
import co.edu.uco.seguridad.pep.normalization.application.port.primary.dto.request.NormalizeAccessRequest;
import co.edu.uco.seguridad.pep.normalization.application.rule.NormalizeAccessRequestMustBeCompleteRule;

/** Validación determinista anterior a crear el mensaje para el PDP. */
public final class NormalizeAccessRequestMustBeCompleteRuleImpl implements NormalizeAccessRequestMustBeCompleteRule {
    @Override
    public void execute(NormalizeAccessRequest input) {
        if (input == null || blank(input.requestId()) || blank(input.correlationId()) || input.timestamp() == null
                || blank(input.applicationId()) || blank(input.environment()) || blank(input.path()) || blank(input.method())) {
            throw new EnforcementFailure(EnforcementFailure.Kind.INVALID_REQUEST, "INVALID_ACCESS_REQUEST");
        }
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
