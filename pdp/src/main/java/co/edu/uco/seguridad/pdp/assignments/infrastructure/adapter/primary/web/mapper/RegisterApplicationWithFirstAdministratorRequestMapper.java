package co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.primary.web.mapper;

import co.edu.uco.seguridad.pdp.applications.application.primaryport.request.RegisterApplicationRequest;
import co.edu.uco.seguridad.pdp.applications.domain.model.ApplicationBaseUrl;
import co.edu.uco.seguridad.pdp.assignments.application.primaryport.request.RegisterApplicationWithFirstAdministratorRequest;
import co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.primary.web.dto.request.raw.RegisterApplicationWithFirstAdministratorRawRequest;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationName;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.commons.model.UserId;
import co.edu.uco.seguridad.shared.web.RequestFieldParser;

/**
 * Mismo patrón que {@code RegisterApplicationRequestMapper} de {@code applications}, más el {@code UserId} del registrador.
 */
public final class RegisterApplicationWithFirstAdministratorRequestMapper {

    private RegisterApplicationWithFirstAdministratorRequestMapper() {
    }

    public static RegisterApplicationWithFirstAdministratorRequest toRequest(
            RegisterApplicationWithFirstAdministratorRawRequest raw, TenantId tenantId, UserId registrarUserId) {
        RegisterApplicationRequest application = new RegisterApplicationRequest(
                tenantId,
                RequestFieldParser.parse("name", raw.name(), ApplicationName::new),
                raw.description() == null ? "" : raw.description().trim(),
                RequestFieldParser.parse("baseUrl", raw.baseUrl(), ApplicationBaseUrl::new));
        return new RegisterApplicationWithFirstAdministratorRequest(application, registrarUserId);
    }
}
