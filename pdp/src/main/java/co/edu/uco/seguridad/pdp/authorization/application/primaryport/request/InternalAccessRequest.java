package co.edu.uco.seguridad.pdp.authorization.application.primaryport.request;

import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.resources.domain.model.HttpVerb;
import co.edu.uco.seguridad.pdp.resources.domain.model.ResourcePath;

/**
 * Entrada de {@link co.edu.uco.seguridad.pdp.authorization.application.usecase.EvaluateInternalAccessUseCase}.
 * Como {@code AccessRequest}, pero sin {@code tenantId}: en el canal interno el inquilino se resuelve
 * antes, a partir de {@code applicationId} (HU-003, decisión D4 del HANDOFF).
 */
public record InternalAccessRequest(String subject, ApplicationId applicationId, ResourcePath resourcePath,
        HttpVerb action, String requestId, String correlationId) {

    public InternalAccessRequest {
    }
}
