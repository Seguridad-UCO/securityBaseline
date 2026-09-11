package co.edu.uco.seguridad.pep.commons;

import java.util.List;

public record AccessDecision(Decision decision, String decisionId, String reasonCode,
                             List<PolicyReference> policyReferences, String correlationId, String requestId,
                             List<String> obligations) {
    public enum Decision {ALLOW, DENY, INDETERMINATE}

    public record PolicyReference(String id, String version) {
    }
}

