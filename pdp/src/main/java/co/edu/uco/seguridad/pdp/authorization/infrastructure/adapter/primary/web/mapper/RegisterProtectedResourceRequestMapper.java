package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.mapper;

import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.request.raw.RegisterProtectedResourceRawRequest;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.resources.application.primaryport.request.RegisterProtectedResourceRequest;
import co.edu.uco.seguridad.pdp.resources.domain.model.HttpVerb;
import co.edu.uco.seguridad.pdp.resources.domain.model.ResourcePath;
import co.edu.uco.seguridad.shared.web.RequestFieldParser;

/**
 * raw a {@code RegisterProtectedResourceRequest} (HU-017): idéntico a
 * {@code resources...RegisterProtectedResourceRequestMapper}. Sin cambios de catálogo de mensajes:
 * este mapper no lanza ningún mensaje propio.
 */
public final class RegisterProtectedResourceRequestMapper {

    private RegisterProtectedResourceRequestMapper() {
    }

    public static RegisterProtectedResourceRequest toRequest(RegisterProtectedResourceRawRequest raw,
                                                             TenantId tenantId) {
        return new RegisterProtectedResourceRequest(
                tenantId,
                RequestFieldParser.parse("applicationId", raw.applicationId(), ApplicationId::of),
                RequestFieldParser.parse("path", raw.path(), ResourcePath::new),
                RequestFieldParser.parse("method", raw.method(), HttpVerb::parse));
    }
}
