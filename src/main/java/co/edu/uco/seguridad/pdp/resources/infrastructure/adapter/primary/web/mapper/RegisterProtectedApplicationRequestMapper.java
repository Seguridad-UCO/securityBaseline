package co.edu.uco.seguridad.pdp.resources.infrastructure.adapter.primary.web.mapper;

import co.edu.uco.seguridad.pdp.commons.ApplicationName;
import co.edu.uco.seguridad.pdp.commons.TenantId;
import co.edu.uco.seguridad.pdp.resources.application.port.primary.dto.request.RegisterProtectedApplicationRequest;
import co.edu.uco.seguridad.pdp.resources.domain.ActionCode;
import co.edu.uco.seguridad.pdp.resources.domain.ResourceCode;
import co.edu.uco.seguridad.pdp.resources.infrastructure.adapter.primary.web.dto.request.raw.RegisterProtectedApplicationRawRequest;
import co.edu.uco.seguridad.shared.web.RequestFieldParser;

/**
 * La puerta entre el mundo exterior y la aplicación: DTO crudo entra, DTO tipado sale. Cada campo se
 * analiza con {@link RequestFieldParser#parse}. {@code tenantId} llega aparte, del token (ADR-018),
 * no de {@code raw}.
 */
public final class RegisterProtectedApplicationRequestMapper {

    private RegisterProtectedApplicationRequestMapper() {
    }

    public static RegisterProtectedApplicationRequest toRequest(
            RegisterProtectedApplicationRawRequest raw, TenantId tenantId) {
        return new RegisterProtectedApplicationRequest(
                tenantId,
                RequestFieldParser.parse("applicationName", raw.applicationName(), ApplicationName::new),
                RequestFieldParser.parse("resourceCode", raw.resourceCode(), ResourceCode::new),
                RequestFieldParser.parse("action", raw.action(), ActionCode::new));
    }
}
