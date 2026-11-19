package co.edu.uco.seguridad.pdp.authorization.application.primaryport.response;

import co.edu.uco.seguridad.pdp.authorization.domain.model.DecisionState;
import co.edu.uco.seguridad.pdp.authorization.domain.model.ReasonCode;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;

import java.util.Objects;

/**
 * Decisión de administración (HU-009): más liviana que {@code AccessDecision} a propósito — sin
 * {@code decisionId}/{@code policyReferences}/{@code decidedAt}, porque esta decisión no se audita
 * (mismo criterio que HU-013 con la validación de credenciales de aplicación).
 */
public record AdministrationDecision(DecisionState state, ReasonCode reasonCode) {

    public AdministrationDecision {
        Objects.requireNonNull(state, RequiredArgumentMessages.DECISION_STATE);
        Objects.requireNonNull(reasonCode, RequiredArgumentMessages.REASON_CODE);
    }

    public boolean permits() {
        return state.isAllow();
    }
}
