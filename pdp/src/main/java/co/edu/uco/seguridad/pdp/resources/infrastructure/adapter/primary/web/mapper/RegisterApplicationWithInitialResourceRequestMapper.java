package co.edu.uco.seguridad.pdp.resources.infrastructure.adapter.primary.web.mapper;

import co.edu.uco.seguridad.pdp.applications.domain.model.ApplicationBaseUrl;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationName;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.resources.application.primaryport.request.RegisterApplicationWithInitialResourceRequest;
import co.edu.uco.seguridad.pdp.resources.domain.model.HttpVerb;
import co.edu.uco.seguridad.pdp.resources.domain.model.ResourcePath;
import co.edu.uco.seguridad.pdp.resources.infrastructure.adapter.primary.web.dto.request.raw.RegisterApplicationWithInitialResourceRawRequest;
import co.edu.uco.seguridad.shared.web.RequestFieldParser;

/**
 * raw a {@link RegisterApplicationWithInitialResourceRequest} con {@code RequestFieldParser}
 * (HU-010). El tenant llega aparte, del principal — nunca del cuerpo.
 */
public final class RegisterApplicationWithInitialResourceRequestMapper {

    private RegisterApplicationWithInitialResourceRequestMapper() {
    }

    public static RegisterApplicationWithInitialResourceRequest toRequest(
            RegisterApplicationWithInitialResourceRawRequest raw, TenantId tenantId) {
        return new RegisterApplicationWithInitialResourceRequest(
                tenantId,
                RequestFieldParser.parse("name", raw.name(), ApplicationName::new),
                raw.description() == null ? "" : raw.description().trim(),
                RequestFieldParser.parse("baseUrl", raw.baseUrl(), ApplicationBaseUrl::new),
                RequestFieldParser.parse("resourcePath", raw.resourcePath(), ResourcePath::new),
                RequestFieldParser.parse("resourceMethod", raw.resourceMethod(), HttpVerb::parse));
    }
}
