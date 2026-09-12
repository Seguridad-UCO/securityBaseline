package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.mapper;

import co.edu.uco.seguridad.pdp.authorization.application.primaryport.response.AccessDecision;
import co.edu.uco.seguridad.pdp.authorization.domain.model.PolicyReference;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.response.AccessDecisionInternalWebResponse;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.response.PolicyReferenceWebResponse;

/**
 * {@link AccessDecision} (núcleo) → {@code AccessDecisionInternalWebResponse} (plano, según
 * {@code decision.schema.json}). Reutiliza {@code PolicyReferenceWebResponse} — mismo tipo que ya
 * usa el canal BFF (HU-002) para el mismo dato.
 */
public final class AccessDecisionInternalResponseMapper {

    private AccessDecisionInternalResponseMapper() {
    }

    public static AccessDecisionInternalWebResponse toResponse(AccessDecision decision) {
        return new AccessDecisionInternalWebResponse(
                decision.state().name(),
                decision.decisionId().toString(),
                decision.reasonCode().name(),
                decision.policyReferences().stream().map(AccessDecisionInternalResponseMapper::toReferenceResponse).toList(),
                decision.requestId(),
                decision.correlationId());
    }

    private static PolicyReferenceWebResponse toReferenceResponse(PolicyReference reference) {
        return new PolicyReferenceWebResponse(reference.policyId(), reference.version());
    }
}
