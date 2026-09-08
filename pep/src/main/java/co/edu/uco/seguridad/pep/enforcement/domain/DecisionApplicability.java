package co.edu.uco.seguridad.pep.enforcement.domain;

import co.edu.uco.seguridad.pep.commons.AccessDecision;
import co.edu.uco.seguridad.pep.commons.AccessRequest;
import co.edu.uco.seguridad.pep.commons.EnforcementFailure;

import static co.edu.uco.seguridad.pep.commons.EnforcementFailure.Kind.*;

/**
 * Verifies whether a remote decision can be applied. Never evaluates authorization policies.
 */
public final class DecisionApplicability {
    private DecisionApplicability() {
    }

    public static void verify(AccessRequest request, AccessDecision result) {
        if (result == null || result.decision() == null || !safe(result.decisionId())
                || !safe(result.reasonCode()) || result.policyReferences() == null
                || result.policyReferences().stream().anyMatch(p -> p == null || !safe(p.id()) || !safe(p.version()))
                || !request.requestId().equals(result.requestId())
                || !request.correlationId().equals(result.correlationId())) {
            throw new EnforcementFailure(UNAVAILABLE, "PDP_INVALID_RESPONSE");
        }
        if (result.obligations() != null && !result.obligations().isEmpty()) {
            throw new EnforcementFailure(UNAVAILABLE, "UNSUPPORTED_OBLIGATION");
        }
        switch (result.decision()) {
            case ALLOW -> {
                if ("TOKEN_INVALID".equals(result.reasonCode())) {
                    throw new EnforcementFailure(UNAVAILABLE, "PDP_INVALID_RESPONSE");
                }
            }
            case DENY -> throw new EnforcementFailure("TOKEN_INVALID".equals(result.reasonCode())
                    ? UNAUTHENTICATED : DENIED, "TOKEN_INVALID".equals(result.reasonCode())
                    ? "TOKEN_INVALID" : "ACCESS_DENIED", result.decisionId());
            case INDETERMINATE ->
                    throw new EnforcementFailure(UNAVAILABLE, "ACCESS_INDETERMINATE", result.decisionId());
        }
    }

    private static boolean safe(String value) {
        return value != null && value.matches("[A-Za-z0-9_.:-]{1,128}");
    }
}
