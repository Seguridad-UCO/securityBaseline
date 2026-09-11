package co.edu.uco.seguridad.pdp.authorization.application.primaryport.response;

import co.edu.uco.seguridad.pdp.authorization.domain.model.DecisionState;
import co.edu.uco.seguridad.pdp.authorization.domain.model.PolicyReference;
import co.edu.uco.seguridad.pdp.authorization.domain.model.ReasonCode;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Decision estructurada (corresponde a {@code DecisionAcceso} del dominio aceptado). */
public record AccessDecision(UUID decisionId, DecisionState state, ReasonCode reasonCode,
        List<PolicyReference> policyReferences, String correlationId, Instant decidedAt) {

    public AccessDecision {
        Objects.requireNonNull(decisionId, RequiredArgumentMessages.DECISION_ID);
        Objects.requireNonNull(state, RequiredArgumentMessages.DECISION_STATE);
        Objects.requireNonNull(reasonCode, RequiredArgumentMessages.REASON_CODE);
        policyReferences = List.copyOf(
                Objects.requireNonNull(policyReferences, RequiredArgumentMessages.POLICY_REFERENCES));
        Objects.requireNonNull(correlationId, RequiredArgumentMessages.CORRELATION_ID);
        Objects.requireNonNull(decidedAt, RequiredArgumentMessages.DECIDED_AT);
    }
}
