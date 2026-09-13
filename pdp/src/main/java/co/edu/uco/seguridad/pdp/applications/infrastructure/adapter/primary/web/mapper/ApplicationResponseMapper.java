package co.edu.uco.seguridad.pdp.applications.infrastructure.adapter.primary.web.mapper;

import co.edu.uco.seguridad.pdp.applications.application.primaryport.response.ApplicationRegistrationResponse;
import co.edu.uco.seguridad.pdp.applications.application.primaryport.response.RegisteredApplicationResponse;
import co.edu.uco.seguridad.pdp.applications.infrastructure.adapter.primary.web.dto.response.ApplicationRegisteredWebResponse;
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

    /** Aplana {@link ApplicationRegistrationResponse}, incluyendo el secreto en claro (HU-012). */
    public static ApplicationRegisteredWebResponse toRegisteredResponse(ApplicationRegistrationResponse response) {
        RegisteredApplicationResponse application = response.application();
        return new ApplicationRegisteredWebResponse(application.id().value().toString(),
                application.tenantId().value(), application.name().value(), application.description(),
                application.baseUrl().value(), response.credential(), application.registeredAt());
    }
}
