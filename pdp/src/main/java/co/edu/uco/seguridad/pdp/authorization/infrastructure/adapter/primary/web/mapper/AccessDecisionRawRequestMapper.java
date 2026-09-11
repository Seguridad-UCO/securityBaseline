package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.mapper;

import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.InternalAccessRequest;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.request.raw.AccessDecisionRawRequest;

/**
 * {@code AccessDecisionRawRequest} (Strings desnudos, según {@code request.schema.json}) →
 * {@link InternalAccessRequest} (value objects). Aplica las barreras C1/C4 de la sección 3 del plan
 * (versión y timestamp) — C2/C3 (coherencia de IDs con los headers) las aplica el interactor, que es
 * quien tiene el {@code RequestContext} de {@code CorrelationWebFilter}.
 */
public final class AccessDecisionRawRequestMapper {

    private AccessDecisionRawRequestMapper() {
    }

    public static InternalAccessRequest toRequest(AccessDecisionRawRequest raw, String subject) {
        throw new UnsupportedOperationException("pendiente: HU-003");
    }
}
