package co.edu.uco.seguridad.pdp.roles.application.primaryport.request;

import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;

public record ApplicationRoleCountRequest(TenantId tenantId, ApplicationId applicationId) {
}
