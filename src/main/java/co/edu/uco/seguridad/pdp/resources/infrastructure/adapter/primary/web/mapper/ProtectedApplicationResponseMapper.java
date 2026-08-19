package co.edu.uco.seguridad.pdp.resources.infrastructure.adapter.primary.web.mapper;

import co.edu.uco.seguridad.pdp.commons.ResultPage;
import co.edu.uco.seguridad.pdp.resources.domain.ProtectedResource;
import co.edu.uco.seguridad.pdp.resources.infrastructure.adapter.primary.web.dto.response.ProtectedApplicationResponse;
import co.edu.uco.seguridad.shared.web.PageResponse;

/**
 * Traducción de salida: dominio a carga útil HTTP. Desenvuelve los objetos de valor y no toma
 * ninguna decisión sobre qué exponer más allá de eso.
 *
 * <p>Mapea directo desde {@link ProtectedResource} — no hay un DTO de aplicación intermedio entre
 * el caso de uso y este mapper. Uno existió, pero tenía exactamente los mismos campos que el
 * agregado (que ya es un {@code record} inmutable); el único efecto observable era un salto de
 * conversión más y un mapper adicional que mantener.</p>
 */
public final class ProtectedApplicationResponseMapper {

    private ProtectedApplicationResponseMapper() {
    }

    public static ProtectedApplicationResponse toResponse(ProtectedResource resource) {
        return new ProtectedApplicationResponse(
                resource.applicationId().value().toString(),
                resource.id().value().toString(),
                resource.tenantId().value(),
                resource.applicationName().value(),
                resource.code().value(),
                resource.action().value(),
                resource.registeredAt());
    }

    public static PageResponse<ProtectedApplicationResponse> toPageResponse(ResultPage<ProtectedResource> page) {
        return new PageResponse<>(
                page.content().stream().map(ProtectedApplicationResponseMapper::toResponse).toList(),
                page.total(),
                page.window().page(),
                page.window().offset(),
                page.window().limit());
    }
}
