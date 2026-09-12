package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.response;

import java.util.List;

/**
 * Espejo de {@code DecisionAcceso v1} ({@code contracts/pep-pdp/v1/decision.schema.json}). Desnudo
 * a propósito (HU-003, decisión D6): el PEP espera el objeto plano en la raíz, no envuelto en
 * {@code ApiResponse} como el resto de la API. {@code obligations} se omite — la v1 del PEP solo
 * admite ausente/null/vacío y hoy no hay obligaciones que transportar.
 */
public record AccessDecisionInternalWebResponse(String decision, String decisionId, String reasonCode,
        List<PolicyReferenceWebResponse> policyReferences, String requestId, String correlationId) {
}
