package co.edu.uco.seguridad.pdp.applications.infrastructure.adapter.primary.web.mapper;

import co.edu.uco.seguridad.pdp.applications.application.port.primary.dto.response.RegisteredApplicationResponse;
import co.edu.uco.seguridad.pdp.applications.infrastructure.adapter.primary.web.dto.response.ApplicationWebResponse;

import java.util.List;

/** Traducción de salida: DTO de aplicación a carga útil HTTP. Desenvuelve los objetos de valor. */
public final class ApplicationResponseMapper {

    private ApplicationResponseMapper() {
    }

    public static ApplicationWebResponse toResponse(RegisteredApplicationResponse response) {
        return new ApplicationWebResponse(response.id().value().toString(), response.tenantId().value(),
                response.name().value(), response.description(), response.baseUrl().value(),
                response.registeredAt());
    }

    public static List<ApplicationWebResponse> toResponseList(List<RegisteredApplicationResponse> responses) {
        return responses.stream().map(ApplicationResponseMapper::toResponse).toList();
    }
}
