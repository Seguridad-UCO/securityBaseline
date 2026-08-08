package co.edu.uco.seguridad.pdp.recursos.infrastructure.adapter.primary.web.mapper;

import co.edu.uco.seguridad.pdp.commons.ResultPage;
import co.edu.uco.seguridad.pdp.recursos.infrastructure.adapter.primary.web.dto.response.ProtectedApplicationResponse;
import co.edu.uco.seguridad.shared.web.PageResponse;

/**
 * Traducción de salida: modelo de lectura del catálogo a carga útil HTTP. Desenvuelve los objetos
 * de valor y no toma ninguna decisión sobre qué exponer más allá de eso.
 */
public final class ProtectedApplicationResponseMapper {

    private ProtectedApplicationResponseMapper() {
    }

    public static ProtectedApplicationResponse toResponse(
            co.edu.uco.seguridad.pdp.recursos.application.port.primary.dto.response.ProtectedApplicationResponse dto) {
        return new ProtectedApplicationResponse(
                dto.applicationId().value().toString(),
                dto.resourceId().value().toString(),
                dto.tenantId().value(),
                dto.applicationName().value(),
                dto.resourceCode().value(),
                dto.action().value(),
                dto.registeredAt());
    }

    public static PageResponse<ProtectedApplicationResponse> toPageResponse(
            ResultPage<co.edu.uco.seguridad.pdp.recursos.application.port.primary.dto.response.ProtectedApplicationResponse> page) {
        return new PageResponse<>(
                page.content().stream().map(ProtectedApplicationResponseMapper::toResponse).toList(),
                page.total(),
                page.window().page(),
                page.window().limit(),
                page.window().offset(),
                page.window().limit());
    }
}
