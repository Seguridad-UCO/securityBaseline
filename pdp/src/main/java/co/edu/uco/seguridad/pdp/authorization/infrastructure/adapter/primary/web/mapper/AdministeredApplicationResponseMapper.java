package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.mapper;

import co.edu.uco.seguridad.pdp.applications.application.primaryport.response.ApplicationRegistrationResponse;
import co.edu.uco.seguridad.pdp.applications.application.primaryport.response.RegisteredApplicationResponse;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.response.AdministeredApplicationWebResponse;

public final class AdministeredApplicationResponseMapper {

    private AdministeredApplicationResponseMapper() {
    }

    public static AdministeredApplicationWebResponse toWebResponse(ApplicationRegistrationResponse response) {
        RegisteredApplicationResponse application = response.application();
        return new AdministeredApplicationWebResponse(application.id().value().toString(),
                application.tenantId().value(), application.name().value(), application.description(),
                application.baseUrl().value(), response.credential(), application.registeredAt());
    }
}
