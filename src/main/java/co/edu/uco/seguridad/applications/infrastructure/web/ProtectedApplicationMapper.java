package co.edu.uco.seguridad.applications.infrastructure.web;

import co.edu.uco.seguridad.applications.application.port.in.RegisterProtectedApplicationCommand;
import co.edu.uco.seguridad.applications.domain.*;

final class ProtectedApplicationMapper {
    private ProtectedApplicationMapper() { }
    static RegisterProtectedApplicationCommand command(RegisterProtectedApplicationRequest request) {
        return new RegisterProtectedApplicationCommand(new ApplicationName(request.name()), new TenantId(request.tenantId()), new ResourceIdentifier(request.resource()));
    }
    static ProtectedApplicationResponse response(ProtectedApplication app) {
        return new ProtectedApplicationResponse(app.id().value(), app.name().value(), app.tenantId().value(), app.resources().getFirst().identifier().value(), "ENABLED", app.registeredAt());
    }
}
