package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.mapper;

import co.edu.uco.seguridad.pdp.authorization.application.primaryport.response.AccessDecision;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.response.AccessDecisionInternalWebResponse;

/**
 * {@link AccessDecision} (núcleo) → {@code AccessDecisionInternalWebResponse} (plano, según
 * {@code decision.schema.json}). Reutiliza {@code PolicyReferenceWebResponse} — mismo tipo que ya
 * usa el canal BFF (HU-002) para el mismo dato.
 */
public final class AccessDecisionInternalResponseMapper {

    private AccessDecisionInternalResponseMapper() {
    }

    public static AccessDecisionInternalWebResponse toResponse(AccessDecision decision) {
        throw new UnsupportedOperationException("pendiente: HU-003");
    }
}
