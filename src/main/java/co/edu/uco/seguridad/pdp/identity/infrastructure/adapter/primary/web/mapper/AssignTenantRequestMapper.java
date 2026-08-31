package co.edu.uco.seguridad.pdp.identity.infrastructure.adapter.primary.web.mapper;

import co.edu.uco.seguridad.pdp.commons.TenantId;
import co.edu.uco.seguridad.pdp.identity.application.primaryport.request.AssignTenantRequest;
import co.edu.uco.seguridad.pdp.identity.domain.UserId;
import co.edu.uco.seguridad.pdp.identity.infrastructure.adapter.primary.web.dto.request.raw.AssignTenantRawRequest;
import co.edu.uco.seguridad.shared.web.RequestFieldParser;

/** La puerta entre el mundo exterior y la aplicación: DTO crudo entra, DTO tipado sale. */
public final class AssignTenantRequestMapper {

    private AssignTenantRequestMapper() {
    }

    public static AssignTenantRequest toRequest(AssignTenantRawRequest raw) {
        return new AssignTenantRequest(
                RequestFieldParser.parse("userId", raw.userId(), UserId::of),
                RequestFieldParser.parse("tenantCode", raw.tenantCode(), TenantId::new));
    }
}
