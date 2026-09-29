package co.edu.uco.seguridad.pdp.profiles.application.primaryport.request;

import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.PageWindow;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;

public record ListApplicationProfilesPageRequest(TenantId tenantId, ApplicationId applicationId, PageWindow window) {
}
