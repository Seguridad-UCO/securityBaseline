package co.edu.uco.seguridad.pdp.applications.infrastructure.adapter.primary.web.mapper;

import co.edu.uco.seguridad.pdp.applications.infrastructure.adapter.primary.web.dto.response.ApplicationCredentialValidationWebResponse;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;

/** Traducción de salida: {@link TenantId} a carga útil HTTP plana (HU-013). */
public final class ApplicationCredentialValidationResponseMapper {

    private ApplicationCredentialValidationResponseMapper() {
    }

    public static ApplicationCredentialValidationWebResponse toResponse(TenantId tenantId) {
        return new ApplicationCredentialValidationWebResponse(tenantId.value());
    }
}
