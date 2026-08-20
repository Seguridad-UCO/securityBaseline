package co.edu.uco.seguridad.pdp.tenants.infrastructure.adapter.primary.web.mapper;

import co.edu.uco.seguridad.pdp.tenants.application.port.primary.dto.response.TenantResponse;
import co.edu.uco.seguridad.pdp.tenants.infrastructure.adapter.primary.web.dto.response.TenantWebResponse;

import java.util.List;

/** Traducción de salida: DTO de aplicación a carga útil HTTP. Desenvuelve los objetos de valor. */
public final class TenantResponseMapper {

    private TenantResponseMapper() {
    }

    public static TenantWebResponse toResponse(TenantResponse tenant) {
        return new TenantWebResponse(tenant.id().value(), tenant.name().value(), tenant.status().name());
    }

    public static List<TenantWebResponse> toResponseList(List<TenantResponse> tenants) {
        return tenants.stream().map(TenantResponseMapper::toResponse).toList();
    }
}
