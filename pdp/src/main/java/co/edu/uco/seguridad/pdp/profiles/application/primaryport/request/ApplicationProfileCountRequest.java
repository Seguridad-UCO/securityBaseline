package co.edu.uco.seguridad.pdp.profiles.application.primaryport.request;

import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;

public record ApplicationProfileCountRequest(TenantId tenantId, ApplicationId applicationId) {
}
