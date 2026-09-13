package co.edu.uco.seguridad.pdp.applications.infrastructure.adapter.primary.web.mapper;

import co.edu.uco.seguridad.pdp.applications.application.primaryport.request.RotateApplicationCredentialRequest;
import co.edu.uco.seguridad.pdp.applications.infrastructure.adapter.primary.web.dto.request.raw.RotateApplicationCredentialRawRequest;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.shared.web.RequestFieldParser;

/**
 * raw a {@link RotateApplicationCredentialRequest} con {@link RequestFieldParser} (HU-014). El
 * tenant llega aparte, del principal — nunca del path ni del cuerpo.
 */
public final class RotateApplicationCredentialRequestMapper {

    private RotateApplicationCredentialRequestMapper() {
    }

    public static RotateApplicationCredentialRequest toRequest(RotateApplicationCredentialRawRequest raw,
            TenantId tenantId) {
        ApplicationId applicationId = RequestFieldParser.parse("applicationId", raw.applicationId(), ApplicationId::of);
        return new RotateApplicationCredentialRequest(tenantId, applicationId);
    }
}
