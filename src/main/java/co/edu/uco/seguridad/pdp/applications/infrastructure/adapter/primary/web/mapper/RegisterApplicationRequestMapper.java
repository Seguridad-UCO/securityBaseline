package co.edu.uco.seguridad.pdp.applications.infrastructure.adapter.primary.web.mapper;

import co.edu.uco.seguridad.pdp.applications.application.primaryport.request.RegisterApplicationRequest;
import co.edu.uco.seguridad.pdp.applications.domain.model.ApplicationBaseUrl;
import co.edu.uco.seguridad.pdp.applications.infrastructure.adapter.primary.web.dto.request.raw.RegisterApplicationRawRequest;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationName;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.shared.web.RequestFieldParser;

/**
 * La puerta entre el mundo exterior y la aplicación: DTO crudo entra, DTO tipado sale. {@code
 * tenantId} llega aparte, del token (ADR-018), no de {@code raw}. {@code description} es texto
 * libre sin invariante de dominio propio, así que solo se normaliza, no se parsea con un value
 * object.
 */
public final class RegisterApplicationRequestMapper {

    private RegisterApplicationRequestMapper() {
    }

    public static RegisterApplicationRequest toRequest(RegisterApplicationRawRequest raw, TenantId tenantId) {
        return new RegisterApplicationRequest(
                tenantId,
                RequestFieldParser.parse("name", raw.name(), ApplicationName::new),
                raw.description() == null ? "" : raw.description().trim(),
                RequestFieldParser.parse("baseUrl", raw.baseUrl(), ApplicationBaseUrl::new));
    }
}
