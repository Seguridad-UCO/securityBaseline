package co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.primary.web.mapper;

import co.edu.uco.seguridad.pdp.applications.application.primaryport.response.ApplicationRegistrationResponse;
import co.edu.uco.seguridad.pdp.applications.application.primaryport.response.RegisteredApplicationResponse;
import co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.primary.web.dto.response.ApplicationRegisteredWebResponse;

public final class ApplicationRegisteredResponseMapper {

    private ApplicationRegisteredResponseMapper() {
    }

    public static ApplicationRegisteredWebResponse toWebResponse(ApplicationRegistrationResponse response) {
        RegisteredApplicationResponse application = response.application();
        return new ApplicationRegisteredWebResponse(application.id().value().toString(),
                application.tenantId().value(), application.name().value(), application.description(),
                application.baseUrl().value(), response.credential(), application.registeredAt());
    }
}
