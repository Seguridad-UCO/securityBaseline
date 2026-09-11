package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.response;

import java.util.List;

/** Respuesta plana: ningun enum ni value object de dominio cruza al cliente. */
public record AccessDecisionWebResponse(String decisionId, String state, String reasonCode,
        List<PolicyReferenceWebResponse> policyReferences, String requestId, String correlationId,
        String decidedAt) {
}
