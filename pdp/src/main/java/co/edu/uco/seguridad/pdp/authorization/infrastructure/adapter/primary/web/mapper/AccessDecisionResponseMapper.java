package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.mapper;

import co.edu.uco.seguridad.pdp.authorization.application.primaryport.response.AccessDecision;
import co.edu.uco.seguridad.pdp.authorization.domain.model.PolicyReference;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.response.AccessDecisionWebResponse;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.response.PolicyReferenceWebResponse;

/** AccessDecision (dominio) -> AccessDecisionWebResponse (plana): ningun enum ni value object cruza al cliente. */
public final class AccessDecisionResponseMapper {

    private AccessDecisionResponseMapper() {
    }

    public static AccessDecisionWebResponse toResponse(AccessDecision decision) {
        return new AccessDecisionWebResponse(
                decision.decisionId().toString(),
                decision.state().name(),
                decision.reasonCode().name(),
                decision.policyReferences().stream().map(AccessDecisionResponseMapper::toReferenceResponse).toList(),
                decision.requestId(),
                decision.correlationId(),
                decision.decidedAt().toString());
    }

    private static PolicyReferenceWebResponse toReferenceResponse(PolicyReference reference) {
        return new PolicyReferenceWebResponse(reference.policyId(), reference.version());
    }
}
