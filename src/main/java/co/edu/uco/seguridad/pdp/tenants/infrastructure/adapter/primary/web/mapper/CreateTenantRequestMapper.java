package co.edu.uco.seguridad.pdp.tenants.infrastructure.adapter.primary.web.mapper;

import co.edu.uco.seguridad.pdp.commons.TenantId;
import co.edu.uco.seguridad.pdp.tenants.application.primaryport.request.CreateTenantRequest;
import co.edu.uco.seguridad.pdp.tenants.domain.TenantName;
import co.edu.uco.seguridad.pdp.tenants.infrastructure.adapter.primary.web.dto.request.raw.CreateTenantRawRequest;
import co.edu.uco.seguridad.shared.web.RequestFieldParser;

/**
 * La puerta entre el mundo exterior y la aplicación: DTO crudo entra, DTO tipado sale. Cada campo se
 * analiza con {@link RequestFieldParser#parse}.
 */
public final class CreateTenantRequestMapper {

    private CreateTenantRequestMapper() {
    }

    public static CreateTenantRequest toRequest(CreateTenantRawRequest raw) {
        return new CreateTenantRequest(
                RequestFieldParser.parse("code", raw.code(), TenantId::new),
                RequestFieldParser.parse("name", raw.name(), TenantName::new));
    }
}
