package co.edu.uco.seguridad.pdp.resources.infrastructure.adapter.primary.web.mapper;

import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.resources.application.primaryport.request.RegisterProtectedResourceRequest;
import co.edu.uco.seguridad.pdp.resources.domain.model.HttpVerb;
import co.edu.uco.seguridad.pdp.resources.domain.model.ResourcePath;
import co.edu.uco.seguridad.pdp.resources.infrastructure.adapter.primary.web.dto.request.raw.RegisterProtectedResourceRawRequest;
import co.edu.uco.seguridad.shared.web.RequestFieldParser;

/**
 * La puerta entre el mundo exterior y la aplicación: DTO crudo entra, DTO tipado sale. {@code
 * tenantId} llega aparte, del token (ADR-018), no de {@code raw}.
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
