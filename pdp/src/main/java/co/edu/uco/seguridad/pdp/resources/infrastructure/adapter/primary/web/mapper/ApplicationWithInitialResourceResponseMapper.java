package co.edu.uco.seguridad.pdp.resources.infrastructure.adapter.primary.web.mapper;

import co.edu.uco.seguridad.pdp.resources.application.primaryport.response.ApplicationWithInitialResourceRegistrationResponse;
import co.edu.uco.seguridad.pdp.resources.infrastructure.adapter.primary.web.dto.response.ApplicationWithInitialResourceWebResponse;

/** Traducción de salida: DTO de aplicación a carga útil HTTP (HU-010). Desenvuelve los objetos de valor. */
public final class ApplicationWithInitialResourceResponseMapper {

    private ApplicationWithInitialResourceResponseMapper() {
    }

    public static ApplicationWithInitialResourceWebResponse toResponse(
            ApplicationWithInitialResourceRegistrationResponse response) {
        var application = response.application().application();
        var resource = response.resource();
        return new ApplicationWithInitialResourceWebResponse(
                application.id().value().toString(),
                application.tenantId().value(),
                application.name().value(),
                application.description(),
                application.baseUrl().value(),
                response.application().credential(),
                application.registeredAt(),
                resource.id().value().toString(),
                resource.path().value(),
                resource.method().name(),
                resource.registeredAt());
    }
}
