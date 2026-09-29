package co.edu.uco.seguridad.pdp.applications.application.primaryport.request;

import co.edu.uco.seguridad.pdp.applications.domain.model.ApplicationBaseUrl;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationName;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;

/**
 * Datos editables de una aplicación; la credencial y su dueño no son editables por PATCH.
 */
public record UpdateApplicationRequest(TenantId tenantId, ApplicationId applicationId, ApplicationName name,
                                       String description, ApplicationBaseUrl baseUrl) {
}
